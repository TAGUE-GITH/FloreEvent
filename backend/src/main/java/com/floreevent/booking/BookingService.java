package com.floreevent.booking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.booking.BookingDtos.BookingRequest;
import com.floreevent.booking.BookingDtos.LineRequest;
import com.floreevent.catalog.Item;
import com.floreevent.catalog.ItemRepository;
import com.floreevent.common.ApiException;
import com.floreevent.common.Dates;
import com.floreevent.common.Terms;
import com.floreevent.customer.Customer;
import com.floreevent.quote.Geocoder;
import com.floreevent.quote.Pricing;
import com.floreevent.user.User;
import com.floreevent.user.UserRepository;

/** US-07 et US-08 : estimation du prix d'une selection, puis demande de devis du client. */
@Service
public class BookingService {

	private final ItemRepository items;
	private final BookingRepository bookings;
	private final UserRepository users;
	private final AvailabilityService availability;
	private final Pricing pricing;
	private final Geocoder geocoder;
	private final double depotLatitude;
	private final double depotLongitude;

	public BookingService(ItemRepository items, BookingRepository bookings, UserRepository users, AvailabilityService availability,
			Pricing pricing, Geocoder geocoder, @Value("${app.depot.latitude}") double depotLatitude,
			@Value("${app.depot.longitude}") double depotLongitude) {
		this.items = items;
		this.bookings = bookings;
		this.users = users;
		this.availability = availability;
		this.pricing = pricing;
		this.geocoder = geocoder;
		this.depotLatitude = depotLatitude;
		this.depotLongitude = depotLongitude;
	}

	/** Prix detaille et disponibilites ; les erreurs de saisie s'affichent sous le champ concerne. */
	@Transactional(readOnly = true)
	public Estimate estimate(BookingRequest request) {
		if (request.endDate().isBefore(request.startDate()))
			throw ApiException.invalidField("endDate", "La date de fin doit être après la date de début.");
		if (!request.startDate().isAfter(Dates.today()))
			throw ApiException.invalidField("startDate", "La location doit commencer au plus tôt demain.");
		boolean delivery = request.deliveryMode() == DeliveryMode.LIVRAISON;
		if (request.installation() && !delivery)
			throw ApiException.invalidField("installation", "L’installation est proposée avec la livraison.");

		String address = null;
		Integer distanceKm = null;
		int deliveryCents = 0;
		if (delivery) {
			if (request.address() == null || request.address().isBlank())
				throw ApiException.invalidField("address", "Indiquez l’adresse de l’événement.");
			Geocoder.Location location = geocoder.locate(request.address().trim()).orElseThrow(() -> ApiException
					.invalidField("address", "Adresse introuvable : vérifiez le numéro, la rue et la ville."));
			address = location.label();
			distanceKm = (int) Math.round(Pricing.roadKm(depotLatitude, depotLongitude, location.latitude(), location.longitude()));
			deliveryCents = pricing.delivery(distanceKm);
		}

		int days = (int) (request.endDate().toEpochDay() - request.startDate().toEpochDay()) + 1;
		List<Estimate.Line> lines = new ArrayList<>();
		List<String> problems = new ArrayList<>();
		int rental = 0;
		int setup = 0;
		long replacementValue = 0;
		for (var entry : quantities(request.lines()).entrySet()) {
			Item item = items.findById(entry.getKey()).filter(Item::isActive)
					.orElseThrow(() -> ApiException.badRequest("Un article de la sélection n'est plus proposé à la location."));
			int quantity = entry.getValue();
			int available = availability.available(item, request.startDate(), request.endDate());
			if (quantity > available) problems.add(item.getName() + " : " + available + " disponible(s) seulement à ces dates.");
			int lineTotal = pricing.rental(item.getDayPriceCents(), days, quantity);
			lines.add(new Estimate.Line(item, quantity, available, lineTotal));
			rental += lineTotal;
			if (request.installation()) setup += item.getSetupPriceCents() * quantity;
			replacementValue += (long) item.getReplacementValueCents() * quantity;
		}
		int total = rental + setup + deliveryCents;
		return new Estimate(request.startDate(), request.endDate(), days, request.deliveryMode(), address, distanceKm,
				request.installation(), lines, rental, setup, deliveryCents, total, Pricing.vatIncluded(total), pricing.deposit(total),
				pricing.securityDeposit(replacementValue), problems);
	}

	/** Les lignes du meme article sont regroupees (ordre de la selection conserve). */
	private static Map<Long, Integer> quantities(List<LineRequest> lines) {
		Map<Long, Integer> merged = new LinkedHashMap<>();
		for (LineRequest line : lines) merged.merge(line.itemId(), line.quantity(), Integer::sum);
		return merged;
	}

	/** Demande de devis : refusee si un article manque, prix figes (R3). Elle ne bloque pas encore le materiel. */
	@Transactional
	public Booking request(BookingRequest request, User principal) {
		User client = reload(principal);
		Estimate estimate = estimate(request);
		if (!estimate.bookable()) throw ApiException.conflict(estimate.problems().getFirst());
		String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
		Booking booking = bookings.save(new Booking(customerOf(client), estimate, note, Terms.VERSION, client));
		booking.assignReference();
		return booking;
	}

	@Transactional(readOnly = true)
	public List<Booking> mine(User principal) {
		return bookings.findByCustomerIdOrderByCreatedAtDescIdDesc(customerOf(reload(principal)).getId());
	}

	/** Un client ne voit que ses reservations : celles des autres sont « introuvables ». */
	@Transactional(readOnly = true)
	public Booking mine(Long id, User principal) {
		Long customerId = customerOf(reload(principal)).getId();
		return bookings.findById(id).filter(b -> b.getCustomer().getId().equals(customerId))
				.orElseThrow(() -> ApiException.notFound("Réservation introuvable."));
	}

	@Transactional
	public Booking cancelByCustomer(Long id, User principal) {
		Booking booking = mine(id, principal);
		booking.cancelByCustomer(reload(principal));
		return booking;
	}

	@Transactional(readOnly = true)
	public List<Booking> all() {
		return bookings.findAllForTeam();
	}

	@Transactional(readOnly = true)
	public Booking get(Long id) {
		return bookings.findById(id).orElseThrow(() -> ApiException.notFound("Réservation introuvable."));
	}

	private User reload(User principal) {
		return users.findById(principal.getId()).orElseThrow(() -> ApiException.notFound("Compte introuvable."));
	}

	private static Customer customerOf(User user) {
		if (user.getCustomer() == null) throw ApiException.badRequest("Réservé aux comptes clients.");
		return user.getCustomer();
	}
}
