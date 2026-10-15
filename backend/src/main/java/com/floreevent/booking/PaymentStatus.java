package com.floreevent.booking;

public enum PaymentStatus {
	/** Page de paiement Stripe ouverte, confirmation pas encore recue. */
	EN_ATTENTE("En attente"),
	PAYE("Payé"),
	/** Rembourse en tout ou partie (refundedCents). */
	REMBOURSE("Remboursé");

	private final String label;

	PaymentStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
