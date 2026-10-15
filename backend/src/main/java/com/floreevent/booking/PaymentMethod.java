package com.floreevent.booking;

/** Moyens de paiement : la carte passe toujours par la page Stripe (R8), les autres sont saisis par l'equipe (US-12). */
public enum PaymentMethod {
	CARTE("Carte bancaire"),
	VIREMENT("Virement"),
	CHEQUE("Chèque"),
	ESPECES("Espèces");

	private final String label;

	PaymentMethod(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
