package com.floreevent.payment;

/**
 * Prestataire de paiement par carte (Stripe). Le site ne voit jamais la carte : le client paie sur la page du prestataire,
 * puis la confirmation arrive par le webhook signe (R8). Simule dans les tests.
 */
public interface PaymentGateway {

	record CheckoutRequest(long bookingId, String reference, String customerEmail, int amountCents, String successUrl,
			String cancelUrl) {
	}

	/** Session de paiement : identifiant et adresse de la page de paiement. */
	record Checkout(String sessionId, String url) {
	}

	Checkout checkout(CheckoutRequest request);

	void refund(String paymentIntentId, int amountCents);

	/** Paiement simule : developpement et demonstration sans compte Stripe. */
	default boolean simulated() {
		return false;
	}
}
