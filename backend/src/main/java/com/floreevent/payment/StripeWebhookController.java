package com.floreevent.payment;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.PaymentService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * R8 : Stripe previent l'API qu'une session est payee. Seuls les messages signes avec le secret du webhook sont pris en
 * compte : personne d'autre ne peut confirmer une reservation.
 */
@RestController
public class StripeWebhookController {

	private static final Logger LOG = LoggerFactory.getLogger(StripeWebhookController.class);

	private final PaymentService payments;
	private final ObjectMapper json;
	private final String secret;

	public StripeWebhookController(PaymentService payments, ObjectMapper json, @Value("${app.stripe.webhook-secret:}") String secret) {
		this.payments = payments;
		this.json = json;
		this.secret = secret;
	}

	@PostMapping("/api/stripe/webhook")
	public ResponseEntity<Void> receive(@RequestBody String payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String signature) {
		if (!StripeSignature.isValid(payload, signature, secret, Instant.now())) {
			LOG.warn("Webhook Stripe refuse : signature absente ou invalide");
			return ResponseEntity.badRequest().build();
		}
		JsonNode event = json.readTree(payload);
		if ("checkout.session.completed".equals(event.path("type").asString())) {
			JsonNode session = event.path("data").path("object");
			if ("paid".equals(session.path("payment_status").asString()))
				payments.completeCheckout(session.path("id").asString(), session.path("payment_intent").asString());
		}
		return ResponseEntity.ok().build();
	}
}
