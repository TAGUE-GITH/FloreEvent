package com.floreevent.booking;

import java.time.Instant;

import com.floreevent.user.User;

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

/** Paiement d'une reservation (centimes TTC). Un paiement par carte n'est valide que par le webhook signe de Stripe (R8). */
@Entity
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentMethod method;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	@Column(name = "amount_cents", nullable = false)
	private int amountCents;

	@Column(name = "refunded_cents", nullable = false)
	private int refundedCents;

	@Column(name = "checkout_session_id")
	private String checkoutSessionId;

	@Column(name = "payment_intent_id")
	private String paymentIntentId;

	private String note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private User author;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "paid_at")
	private Instant paidAt;

	protected Payment() {
	}

	/** Page de paiement Stripe ouverte : en attente de la confirmation signee. */
	static Payment checkout(Booking booking, int amountCents, String sessionId) {
		var payment = new Payment();
		payment.booking = booking;
		payment.method = PaymentMethod.CARTE;
		payment.status = PaymentStatus.EN_ATTENTE;
		payment.amountCents = amountCents;
		payment.checkoutSessionId = sessionId;
		booking.getPayments().add(payment);
		return payment;
	}

	/** US-12 : paiement hors ligne enregistre par l'equipe, encaisse tout de suite. */
	static Payment offline(Booking booking, PaymentMethod method, int amountCents, String note, User author) {
		var payment = new Payment();
		payment.booking = booking;
		payment.method = method;
		payment.status = PaymentStatus.PAYE;
		payment.amountCents = amountCents;
		payment.note = note;
		payment.author = author;
		payment.paidAt = Instant.now();
		booking.getPayments().add(payment);
		return payment;
	}

	void markPaid(String paymentIntentId) {
		this.status = PaymentStatus.PAYE;
		this.paymentIntentId = paymentIntentId;
		this.paidAt = Instant.now();
	}

	/** Remboursement partiel ou total. */
	void refund(int cents) {
		refundedCents += cents;
		status = PaymentStatus.REMBOURSE;
	}

	/** Somme reellement gardee : rien tant que Stripe n'a pas confirme. */
	public int getNetCents() {
		return status == PaymentStatus.EN_ATTENTE ? 0 : amountCents - refundedCents;
	}

	public Long getId() {
		return id;
	}

	public Booking getBooking() {
		return booking;
	}

	public PaymentMethod getMethod() {
		return method;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public int getAmountCents() {
		return amountCents;
	}

	public int getRefundedCents() {
		return refundedCents;
	}

	public String getCheckoutSessionId() {
		return checkoutSessionId;
	}

	public String getPaymentIntentId() {
		return paymentIntentId;
	}

	public String getNote() {
		return note;
	}

	public User getAuthor() {
		return author;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getPaidAt() {
		return paidAt;
	}
}
