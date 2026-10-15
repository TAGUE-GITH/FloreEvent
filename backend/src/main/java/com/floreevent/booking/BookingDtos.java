package com.floreevent.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.floreevent.catalog.CatalogDtos.Named;
import com.floreevent.catalog.Item;
import com.floreevent.image.Image;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Donnees echangees avec l'API pour le catalogue public, les estimations et les reservations (centimes TTC). */
public final class BookingDtos {

	private BookingDtos() {
	}

	public record LineRequest(
			@NotNull(message = "Choisissez un article.") Long itemId,
			@NotNull(message = "Indiquez la quantité.") @Positive(message = "La quantité doit être d'au moins 1.") Integer quantity) {
	}

	/** Selection du client : sert a l'estimation (sans compte) et a la demande de devis (compte client). */
	public record BookingRequest(
			@NotNull(message = "Choisissez la date de début.") LocalDate startDate,
			@NotNull(message = "Choisissez la date de fin.") LocalDate endDate,
			@NotNull(message = "Choisissez le retrait ou la livraison.") DeliveryMode deliveryMode,
			@Size(max = 255) String address,
			boolean installation,
			@Size(max = 1000, message = "1000 caractères maximum.") String note,
			@NotEmpty(message = "Ajoutez au moins un article.") List<@Valid LineRequest> lines) {
	}

	/** Article tel que les visiteurs le voient : ni reference interne, ni valeur de remplacement. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record PublicItem(Long id, String name, Named category, String description, int dayPriceCents, int setupPriceCents,
			String imageUrl, List<String> images, Integer available) {

		/** available : exemplaires libres aux dates choisies (absent sans dates). */
		public static PublicItem of(Item item, Integer available) {
			return new PublicItem(item.getId(), item.getName(), new Named(item.getCategory().getId(), item.getCategory().getName()),
					item.getDescription(), item.getDayPriceCents(), item.getSetupPriceCents(), Image.url(item.getMainImage()),
					item.getImages().stream().map(photo -> Image.url(photo.getImage())).toList(), available);
		}
	}

	public record EstimateLine(Long itemId, String name, int quantity, int dayPriceCents, int lineTotalCents, int available) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EstimateResponse(LocalDate startDate, LocalDate endDate, int days, DeliveryMode deliveryMode, String address,
			Integer distanceKm, boolean installation, List<EstimateLine> lines, int rentalCents, int setupCents, int deliveryCents,
			int totalCents, int vatCents, int depositCents, int securityDepositCents, boolean bookable, List<String> problems) {

		public static EstimateResponse of(Estimate e) {
			return new EstimateResponse(e.startDate(), e.endDate(), e.days(), e.deliveryMode(), e.address(), e.distanceKm(),
					e.installation(),
					e.lines().stream().map(l -> new EstimateLine(l.item().getId(), l.item().getName(), l.quantity(),
							l.item().getDayPriceCents(), l.lineTotalCents(), l.available())).toList(),
					e.rentalCents(), e.setupCents(), e.deliveryCents(), e.totalCents(), e.vatCents(), e.depositCents(),
					e.securityDepositCents(), e.bookable(), e.problems());
		}
	}

	public record CustomerInfo(Long id, String fullName, String company, String phone, String email) {
	}

	public record LineResponse(Long itemId, String itemName, String imageUrl, int quantity, int dayPriceCents, int lineTotalCents) {
	}

	public record EventResponse(BookingStatus status, String label, String note, String author, Instant createdAt) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record BookingResponse(Long id, String reference, BookingStatus status, String statusLabel, CustomerInfo customer,
			LocalDate startDate, LocalDate endDate, int days, DeliveryMode deliveryMode, String address, Integer distanceKm,
			boolean installation, String note, List<LineResponse> lines, int rentalCents, int setupCents, int deliveryCents,
			int totalCents, int vatCents, int depositCents, int securityDepositCents, Instant createdAt, List<EventResponse> events) {

		/** Les noms des employes ne sont montres qu'a l'equipe. */
		public static BookingResponse of(Booking b, boolean forTeam) {
			var c = b.getCustomer();
			return new BookingResponse(b.getId(), b.getReference(), b.getStatus(), b.getStatus().label(),
					new CustomerInfo(c.getId(), c.getFullName(), c.getCompany(), c.getPhone(), c.getEmail()), b.getStartDate(),
					b.getEndDate(), b.getDays(), b.getDeliveryMode(), b.getAddress(), b.getDistanceKm(), b.isInstallation(), b.getNote(),
					b.getLines().stream().map(l -> new LineResponse(l.getItem().getId(), l.getItemName(), Image.url(l.getItem().getMainImage()),
							l.getQuantity(), l.getDayPriceCents(), l.getLineTotalCents())).toList(),
					b.getRentalCents(), b.getSetupCents(), b.getDeliveryCents(), b.getTotalCents(),
					com.floreevent.quote.Pricing.vatIncluded(b.getTotalCents()), b.getDepositCents(), b.getSecurityDepositCents(),
					b.getCreatedAt(),
					b.getEvents().stream().map(e -> new EventResponse(e.getStatus(), e.getStatus().label(), e.getNote(),
							forTeam && e.getAuthor() != null ? e.getAuthor().getFullName() : null, e.getCreatedAt())).toList());
		}
	}

	/** Version courte pour les listes. */
	public record BookingSummary(Long id, String reference, BookingStatus status, String statusLabel, String customer,
			LocalDate startDate, LocalDate endDate, DeliveryMode deliveryMode, int totalCents, int itemCount, Instant createdAt) {

		public static BookingSummary of(Booking b) {
			return new BookingSummary(b.getId(), b.getReference(), b.getStatus(), b.getStatus().label(), b.getCustomer().getFullName(),
					b.getStartDate(), b.getEndDate(), b.getDeliveryMode(), b.getTotalCents(),
					b.getLines().stream().mapToInt(BookingLine::getQuantity).sum(), b.getCreatedAt());
		}
	}
}
