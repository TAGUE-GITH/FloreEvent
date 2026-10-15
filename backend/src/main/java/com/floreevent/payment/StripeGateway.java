package com.floreevent.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.floreevent.common.ApiException;

import tools.jackson.databind.JsonNode;

/** Stripe Checkout (mode test) par son API HTTP : la cle secrete est saisie chez l'hebergeur, jamais dans le code. */
class StripeGateway implements PaymentGateway {

	private static final Logger LOG = LoggerFactory.getLogger(StripeGateway.class);

	private final RestClient http;

	StripeGateway(String secretKey) {
		this.http = RestClient.builder().baseUrl("https://api.stripe.com/v1").defaultHeader("Authorization", "Bearer " + secretKey)
				.build();
	}

	@Override
	public Checkout checkout(CheckoutRequest request) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("mode", "payment");
		form.add("success_url", request.successUrl());
		form.add("cancel_url", request.cancelUrl());
		form.add("customer_email", request.customerEmail());
		form.add("client_reference_id", String.valueOf(request.bookingId()));
		form.add("metadata[booking_id]", String.valueOf(request.bookingId()));
		form.add("line_items[0][quantity]", "1");
		form.add("line_items[0][price_data][currency]", "eur");
		form.add("line_items[0][price_data][unit_amount]", String.valueOf(request.amountCents()));
		form.add("line_items[0][price_data][product_data][name]", "Acompte de la réservation " + request.reference());
		try {
			JsonNode session = http.post().uri("/checkout/sessions").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
					.retrieve().body(JsonNode.class);
			return new Checkout(session.get("id").asString(), session.get("url").asString());
		} catch (RestClientException e) {
			LOG.error("Stripe : creation de la session de paiement impossible", e);
			throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
					"Le paiement en ligne est momentanément indisponible. Réessayez dans quelques minutes.");
		}
	}

	@Override
	public void refund(String paymentIntentId, int amountCents) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("payment_intent", paymentIntentId);
		form.add("amount", String.valueOf(amountCents));
		http.post().uri("/refunds").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
	}
}
