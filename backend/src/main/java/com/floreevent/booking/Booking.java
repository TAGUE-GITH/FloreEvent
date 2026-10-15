package com.floreevent.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.floreevent.common.ApiException;
import com.floreevent.common.Dates;
import com.floreevent.customer.Customer;
import com.floreevent.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Reservation de materiel pour un evenement : d'abord une demande de devis, puis confirmee par l'acompte.
 * Montants TTC en centimes (R5), figes a la demande (R3).
 */
@Entity
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String reference;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status = BookingStatus.DEMANDE;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_mode", nullable = false)
	private DeliveryMode deliveryMode;

	private String address;

	@Column(name = "distance_km")
	private Integer distanceKm;

	@Column(nullable = false)
	private boolean installation;

	private String note;

	@Column(name = "rental_cents", nullable = false)
	private int rentalCents;

	@Column(name = "setup_cents", nullable = false)
	private int setupCents;

	@Column(name = "delivery_cents", nullable = false)
	private int deliveryCents;

	@Column(name = "total_cents", nullable = false)
	private int totalCents;

	@Column(name = "deposit_cents", nullable = false)
	private int depositCents;

	@Column(name = "security_deposit_cents", nullable = false)
	private int securityDepositCents;

	@Column(name = "terms_version")
	private String termsVersion;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = createdAt;

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<BookingLine> lines = new ArrayList<>();

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<BookingEvent> events = new ArrayList<>();

	protected Booking() {
	}

	/** Demande de devis creee a partir d'une estimation (prix figes). */
	Booking(Customer customer, Estimate estimate, String note, String termsVersion, User author) {
		this.customer = customer;
		this.startDate = estimate.startDate();
		this.endDate = estimate.endDate();
		this.deliveryMode = estimate.deliveryMode();
		this.address = estimate.address();
		this.distanceKm = estimate.distanceKm();
		this.installation = estimate.installation();
		this.note = note;
		this.rentalCents = estimate.rentalCents();
		this.setupCents = estimate.setupCents();
		this.deliveryCents = estimate.deliveryCents();
		this.totalCents = estimate.totalCents();
		this.depositCents = estimate.depositCents();
		this.securityDepositCents = estimate.securityDepositCents();
		this.termsVersion = termsVersion;
		for (Estimate.Line line : estimate.lines()) lines.add(new BookingLine(this, line.item(), line.quantity(), line.lineTotalCents()));
		events.add(new BookingEvent(this, BookingStatus.DEMANDE, null, author));
	}

	/** Reference lisible attribuee apres l'enregistrement : RES-2026-00042. */
	void assignReference() {
		reference = "RES-%d-%05d".formatted(createdAt.atZone(Dates.ZONE).getYear(), id);
	}

	/** Le client retire sa demande tant que rien n'est confirme. */
	void cancelByCustomer(User author) {
		if (status != BookingStatus.DEMANDE && status != BookingStatus.DEVIS)
			throw ApiException.badRequest("Cette réservation est confirmée : contactez-nous pour l'annuler.");
		moveTo(BookingStatus.ANNULEE, "Annulée par le client", author);
	}

	private void moveTo(BookingStatus target, String note, User author) {
		status = target;
		updatedAt = Instant.now();
		events.add(new BookingEvent(this, target, note, author));
	}

	public int getDays() {
		return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
	}

	public Long getId() {
		return id;
	}

	public String getReference() {
		return reference;
	}

	public Customer getCustomer() {
		return customer;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public DeliveryMode getDeliveryMode() {
		return deliveryMode;
	}

	public String getAddress() {
		return address;
	}

	public Integer getDistanceKm() {
		return distanceKm;
	}

	public boolean isInstallation() {
		return installation;
	}

	public String getNote() {
		return note;
	}

	public int getRentalCents() {
		return rentalCents;
	}

	public int getSetupCents() {
		return setupCents;
	}

	public int getDeliveryCents() {
		return deliveryCents;
	}

	public int getTotalCents() {
		return totalCents;
	}

	public int getDepositCents() {
		return depositCents;
	}

	public int getSecurityDepositCents() {
		return securityDepositCents;
	}

	public String getTermsVersion() {
		return termsVersion;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<BookingLine> getLines() {
		return lines;
	}

	public List<BookingEvent> getEvents() {
		return events;
	}
}
