package com.floreevent.booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.booking.BookingDtos.LineResponse;
import com.floreevent.booking.BookingDtos.PlanningTask;
import com.floreevent.booking.BookingDtos.ReturnLine;
import com.floreevent.booking.BookingDtos.ReturnRequest;
import com.floreevent.catalog.Item;
import com.floreevent.catalog.ItemRepository;
import com.floreevent.catalog.Maintenance;
import com.floreevent.catalog.MaintenanceRepository;
import com.floreevent.common.ApiException;
import com.floreevent.common.Dates;
import com.floreevent.common.Money;
import com.floreevent.user.User;

/** US-14 a US-17 : planning, livraison avec caution, retour et etat des lieux, casse retenue sur la caution (R7). */
@Service
public class LogisticsService {

	private static final List<BookingStatus> PLANNED = List.of(BookingStatus.CONFIRMEE, BookingStatus.EN_COURS, BookingStatus.RETOURNEE,
			BookingStatus.CLOTUREE);
	private static final int MAX_PLANNING_DAYS = 62;

	private final BookingRepository bookings;
	private final ItemRepository items;
	private final MaintenanceRepository maintenances;
	private final PaymentRepository payments;
	private final InvoiceService invoices;

	public LogisticsService(BookingRepository bookings, ItemRepository items, MaintenanceRepository maintenances, PaymentRepository payments,
			InvoiceService invoices) {
		this.bookings = bookings;
		this.items = items;
		this.maintenances = maintenances;
		this.payments = payments;
		this.invoices = invoices;
	}

	/** Departs (livraison ou retrait) le premier jour, retours (reprise ou retour) le dernier jour, par date. */
	@Transactional(readOnly = true)
	public List<PlanningTask> planning(LocalDate from, LocalDate to) {
		if (to.isBefore(from)) throw ApiException.badRequest("La date de fin doit être après la date de début.");
		if (to.toEpochDay() - from.toEpochDay() >= MAX_PLANNING_DAYS) throw ApiException.badRequest("Choisissez une période de deux mois au plus.");
		List<PlanningTask> tasks = new ArrayList<>();
		for (Booking b : bookings.findForPlanning(PLANNED, from, to)) {
			boolean delivery = b.getDeliveryMode() == DeliveryMode.LIVRAISON;
			if (!b.getStartDate().isBefore(from) && !b.getStartDate().isAfter(to))
				tasks.add(task(b, b.getStartDate(), delivery ? "LIVRAISON" : "RETRAIT", delivery ? "Livraison" : "Retrait à l’entrepôt",
						b.getStatus() != BookingStatus.CONFIRMEE));
			if (!b.getEndDate().isBefore(from) && !b.getEndDate().isAfter(to))
				tasks.add(task(b, b.getEndDate(), delivery ? "REPRISE" : "RETOUR", delivery ? "Reprise" : "Retour à l’entrepôt",
						b.getStatus() == BookingStatus.RETOURNEE || b.getStatus() == BookingStatus.CLOTUREE));
		}
		// Par jour, les retours avant les departs (le materiel rendu le matin peut repartir), puis par reference
		tasks.sort(Comparator.comparing(PlanningTask::date)
				.thenComparing(t -> t.kind().equals("REPRISE") || t.kind().equals("RETOUR") ? 0 : 1)
				.thenComparing(PlanningTask::reference));
		return tasks;
	}

	private static PlanningTask task(Booking b, LocalDate date, String kind, String label, boolean done) {
		return new PlanningTask(date, kind, label, done, b.getId(), b.getReference(), b.getStatus(), b.getCustomer().getFullName(),
				b.getCustomer().getPhone(), b.getAddress(), b.isInstallation(), b.getLines().stream().map(LineResponse::of).toList());
	}

	/** US-16 : le materiel part (au plus tot la veille) ; la caution est recue, sans etre encaissee. */
	@Transactional
	public Booking deliver(Booking booking, PaymentMethod depositMethod, User author) {
		if (depositMethod == PaymentMethod.CARTE)
			throw ApiException.invalidField("securityDepositMethod", "La caution se remet par chèque, espèces ou virement.");
		if (booking.getStatus() == BookingStatus.CONFIRMEE && Dates.today().isBefore(booking.getStartDate().minusDays(1)))
			throw ApiException.badRequest("Le matériel part au plus tôt la veille de l'événement.");
		String what = booking.getDeliveryMode() == DeliveryMode.LIVRAISON ? "Matériel livré" : "Matériel remis au client";
		booking.deliver(depositMethod, what + ", caution de " + Money.euros(booking.getSecurityDepositCents()) + " reçue ("
				+ depositMethod.label().toLowerCase() + ")", author);
		return booking;
	}

	/**
	 * US-17 : etat des lieux. Chaque article loue est compte : rendu, abime ou manquant. Les abimes partent en reparation,
	 * les manquants sortent du stock ; leur valeur de remplacement est facturee et retenue sur la caution (R7).
	 */
	@Transactional
	public Booking recordReturn(Booking booking, ReturnRequest request, User author) {
		if (booking.getStatus() != BookingStatus.EN_COURS) throw ApiException.badRequest("Le matériel n'a pas encore été livré.");
		Map<Long, ReturnLine> counted = new HashMap<>();
		for (ReturnLine line : request.lines()) counted.put(line.itemId(), line);
		for (BookingLine line : booking.getLines()) {
			ReturnLine count = counted.get(line.getItem().getId());
			if (count == null || count.returned() + count.damaged() + count.missing() != line.getQuantity())
				throw ApiException.badRequest(line.getItemName() + " : " + line.getQuantity()
						+ " loué(s), le total rendu + abîmé + manquant doit faire " + line.getQuantity() + ".");
		}

		String reason = "Abîmé au retour de " + booking.getReference();
		int damage = 0;
		for (BookingLine line : booking.getLines()) {
			ReturnLine count = counted.get(line.getItem().getId());
			line.inspect(count.returned(), count.damaged(), count.missing());
			damage += line.damageCents();
			if (count.damaged() > 0)
				maintenances.save(new Maintenance(line.getItem(), count.damaged(), Dates.today(), null, reason, author));
			if (count.missing() > 0) {
				Item item = items.findByIdForUpdate(line.getItem().getId()).orElseThrow();
				item.setQuantity(Math.max(0, item.getQuantity() - count.missing()));
			}
		}

		int retained = Math.min(damage, booking.getSecurityDepositCents());
		String note = damage == 0 ? "Matériel rendu complet et en bon état"
				: "Casse et manquants : " + Money.euros(damage) + ", dont " + Money.euros(retained) + " retenus sur la caution";
		if (request.note() != null && !request.note().isBlank()) note += ". " + request.note().trim();
		booking.recordReturn(damage, booking.getSecurityDepositCents() - retained, note, author);
		if (retained > 0) {
			Payment retention = payments.save(Payment.offline(booking, booking.getSecurityDepositMethod(), retained,
					"Retenue sur la caution (casse)", author));
			invoices.forPayment(retention);
		}
		return booking;
	}

	/** Dossier termine : tout est paye, la caution (moins la casse) est rendue au client. */
	@Transactional
	public Booking close(Booking booking, User author) {
		if (booking.getStatus() == BookingStatus.RETOURNEE && booking.getBalanceCents() > 0)
			throw ApiException.badRequest("Il reste " + Money.euros(booking.getBalanceCents()) + " à payer avant de clôturer.");
		Integer back = booking.getSecurityDepositReturnedCents();
		booking.close("Caution restituée : " + Money.euros(back == null ? 0 : back), author);
		return booking;
	}
}
