package com.floreevent.payment;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.floreevent.common.ApiException;

/**
 * Choix du prestataire : Stripe si la cle secrete est renseignee ; sinon paiement simule (developpement, demonstration)
 * si app.payments.simulation=true ; sinon le paiement en ligne est desactive.
 */
@Configuration
class PaymentConfig {

	@Bean
	PaymentGateway paymentGateway(@Value("${app.stripe.secret-key:}") String secretKey,
			@Value("${app.payments.simulation:false}") boolean simulation, @Value("${app.frontend-url}") String frontendUrl) {
		if (!secretKey.isBlank()) return new StripeGateway(secretKey);
		if (simulation) return new SimulatedGateway(frontendUrl);
		return new PaymentGateway() {
			@Override
			public Checkout checkout(CheckoutRequest request) {
				throw ApiException.badRequest("Le paiement en ligne n'est pas encore activé : contactez-nous pour régler l'acompte.");
			}

			@Override
			public void refund(String paymentIntentId, int amountCents) {
				// Aucun paiement par carte ne peut exister sans prestataire
			}
		};
	}

	/** Page de paiement de demonstration du site : aucun argent n'est echange. */
	record SimulatedGateway(String frontendUrl) implements PaymentGateway {

		@Override
		public Checkout checkout(CheckoutRequest request) {
			String session = "cs_simule_" + UUID.randomUUID().toString().replace("-", "");
			return new Checkout(session, frontendUrl + "/paiement-simule?session=" + session);
		}

		@Override
		public void refund(String paymentIntentId, int amountCents) {
			// Rien a rembourser : le paiement etait simule
		}

		@Override
		public boolean simulated() {
			return true;
		}
	}
}
