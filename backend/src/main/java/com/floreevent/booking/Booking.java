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

	/** US-18 : prestations demandees (decoration florale...). */
	@Column(name = "prestations_cents", nullable = false)
	private int prestationsCents;

	@Column(name = "total_cents", nullable = false)
	private int totalCents;

	@Column(name = "deposit_cents", nullable = false)
	private int depositCents;

	@Column(name = "security_deposit_cents", nullable = false)
	private int securityDepositCents;

	@Column(name = "terms_version")
	private String termsVersion;

	/** US-09 : remise accordee par l'equipe (deja deduite du total). */
	@Column(name = "discount_cents", nullable = false)
	private int discountCents;

	@Column(name = "quote_message")
	private String quoteMessage;

	/** Le client accepte le devis et paie l'acompte jusqu'a cette date incluse. */
	@Column(name = "quote_valid_until")
	private LocalDate quoteValidUntil;

	/** US-13 : somme gardee quand une reservation confirmee est annulee. */
	@Column(name = "cancellation_fee_cents")
	private Integer cancellationFeeCents;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	@Column(name = "returned_at")
	private Instant returnedAt;

	/** US-16 : caution remise a la livraison (non encaissee). */
	@Enumerated(EnumType.STRING)
	@Column(name = "security_deposit_method")
	private PaymentMethod securityDepositMethod;

	/** Caution rendue au client : la caution moins la casse retenue (R7). */
	@Column(name = "security_deposit_returned_cents")
	private Integer securityDepositReturnedCents;

	/** US-17 : casse et manquants factures au retour. */
	@Column(name = "damage_cents", nullable = false)
	private int damageCents;

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

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<BookingPrestation> prestations = new ArrayList<>();

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<Payment> payments = new ArrayList<>();

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<Invoice> invoices = new ArrayList<>();

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
		this.prestationsCents = estimate.prestationsCents();
		this.totalCents = estimate.totalCents();
		this.depositCents = estimate.depositCents();
		this.securityDepositCents = estimate.securityDepositCents();
		this.termsVersion = termsVersion;
		for (Estimate.Line line : estimate.lines()) lines.add(new BookingLine(this, line.item(), line.quantity(), line.lineTotalCents()));
		for (var prestation : estimate.prestations()) prestations.add(new BookingPrestation(this, prestation));
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

	/**
	 * US-09 : l'equipe envoie le devis, ou le modifie tant qu'il n'est pas accepte. La remise est deduite du total ;
	 * l'acompte est recalcule sur le nouveau total.
	 */
	void sendQuote(int discountCents, String message, LocalDate validUntil, int depositCents, User author) {
		if (status != BookingStatus.DEMANDE && status != BookingStatus.DEVIS)
			throw ApiException.badRequest("Ce devis ne peut plus être modifié.");
		boolean first = status == BookingStatus.DEMANDE;
		this.discountCents = discountCents;
		this.totalCents = grossCents() - discountCents;
		this.depositCents = depositCents;
		this.quoteMessage = message;
		this.quoteValidUntil = validUntil;
		moveTo(BookingStatus.DEVIS, first ? "Devis envoyé au client" : "Devis modifié", author);
	}

	/** Total avant remise : location, installation et livraison. */
	int grossCents() {
		return rentalCents + setupCents + deliveryCents + prestationsCents;
	}

	/** L'equipe ne donne pas suite a une demande (ou retire un devis). */
	void refuse(String reason, User author) {
		if (status != BookingStatus.DEMANDE && status != BookingStatus.DEVIS)
			throw ApiException.badRequest("Cette réservation est confirmée : utilisez l'annulation.");
		moveTo(BookingStatus.ANNULEE, "Annulée par FloreEvent : " + reason, author);
	}

	/** Acompte recu : le materiel est bloque (R1). La disponibilite a ete reverifiee sous verrou (R2). */
	void confirm(String note, User author) {
		if (status != BookingStatus.DEVIS) throw ApiException.badRequest("Seul un devis envoyé peut être confirmé.");
		moveTo(BookingStatus.CONFIRMEE, note, author);
	}

	/** US-13 : annulation d'une reservation confirmee ; fee = somme gardee selon la date. */
	void cancelConfirmed(int fee, String note, User author) {
		if (status != BookingStatus.CONFIRMEE)
			throw ApiException.badRequest("Le matériel est déjà livré : cette réservation ne peut plus être annulée.");
		cancellationFeeCents = fee;
		moveTo(BookingStatus.ANNULEE, note, author);
	}

	/** US-16 : materiel livre (ou retire), caution recue. */
	void deliver(PaymentMethod depositMethod, String note, User author) {
		if (status != BookingStatus.CONFIRMEE) throw ApiException.badRequest("Seule une réservation confirmée peut être livrée.");
		securityDepositMethod = depositMethod;
		deliveredAt = Instant.now();
		moveTo(BookingStatus.EN_COURS, note, author);
	}

	/** US-17 : materiel rendu ; la casse s'ajoute au montant du, la caution rendue est connue. */
	void recordReturn(int damageCents, int securityDepositReturnedCents, String note, User author) {
		if (status != BookingStatus.EN_COURS) throw ApiException.badRequest("Le matériel n'a pas encore été livré.");
		this.damageCents = damageCents;
		this.securityDepositReturnedCents = securityDepositReturnedCents;
		returnedAt = Instant.now();
		moveTo(BookingStatus.RETOURNEE, note, author);
	}

	/** Tout est paye et la caution rendue : dossier termine. */
	void close(String note, User author) {
		if (status != BookingStatus.RETOURNEE) throw ApiException.badRequest("Le matériel n'est pas encore rendu.");
		moveTo(BookingStatus.CLOTUREE, note, author);
	}

	/** Evenement de l'historique sans changement d'etat (paiement rembourse, paiement recu...). */
	void addNote(String note, User author) {
		updatedAt = Instant.now();
		events.add(new BookingEvent(this, status, note, author));
	}

	boolean isQuoteExpired() {
		return status == BookingStatus.DEVIS && quoteValidUntil != null && quoteValidUntil.isBefore(Dates.today());
	}

	/** Somme due : le total, ou apres annulation la part gardee. */
	public int getAmountDueCents() {
		if (status == BookingStatus.ANNULEE) return cancellationFeeCents == null ? 0 : cancellationFeeCents;
		return totalCents + damageCents;
	}

	/** Paiements encaisses, remboursements deduits. */
	public int getPaidCents() {
		return payments.stream().mapToInt(Payment::getNetCents).sum();
	}

	public int getBalanceCents() {
		return getAmountDueCents() - getPaidCents();
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

	public int getPrestationsCents() {
		return prestationsCents;
	}

	public List<BookingPrestation> getPrestations() {
		return prestations;
	}

	public int getDiscountCents() {
		return discountCents;
	}

	public String getQuoteMessage() {
		return quoteMessage;
	}

	public LocalDate getQuoteValidUntil() {
		return quoteValidUntil;
	}

	public Integer getCancellationFeeCents() {
		return cancellationFeeCents;
	}

	public Instant getDeliveredAt() {
		return deliveredAt;
	}

	public Instant getReturnedAt() {
		return returnedAt;
	}

	public PaymentMethod getSecurityDepositMethod() {
		return securityDepositMethod;
	}

	public Integer getSecurityDepositReturnedCents() {
		return securityDepositReturnedCents;
	}

	public int getDamageCents() {
		return damageCents;
	}

	public List<Payment> getPayments() {
		return payments;
	}

	public List<Invoice> getInvoices() {
		return invoices;
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
