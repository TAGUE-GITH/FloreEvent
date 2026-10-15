package com.floreevent.booking;

import java.time.Instant;

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

/** Facture (acompte ou solde) emise pour chaque paiement encaisse, avoir pour chaque remboursement (R6). */
@Entity
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String number;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id")
	private Payment payment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InvoiceKind kind;

	/** TTC en centimes, negatif pour un avoir. */
	@Column(name = "amount_cents", nullable = false)
	private int amountCents;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt = Instant.now();

	protected Invoice() {
	}

	Invoice(String number, Booking booking, Payment payment, InvoiceKind kind, int amountCents) {
		this.number = number;
		this.booking = booking;
		this.payment = payment;
		this.kind = kind;
		this.amountCents = amountCents;
		booking.getInvoices().add(this);
	}

	public Long getId() {
		return id;
	}

	public String getNumber() {
		return number;
	}

	public Booking getBooking() {
		return booking;
	}

	public Payment getPayment() {
		return payment;
	}

	public InvoiceKind getKind() {
		return kind;
	}

	public int getAmountCents() {
		return amountCents;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}
}
