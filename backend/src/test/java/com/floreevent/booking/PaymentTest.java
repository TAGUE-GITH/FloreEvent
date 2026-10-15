package com.floreevent.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import com.floreevent.common.Dates;
import com.floreevent.payment.PaymentGateway;
import com.floreevent.payment.StripeSignature;
import com.floreevent.user.Role;

/** US-10 : acompte par carte (Stripe) confirme par le webhook signe (R8), disponibilite reverifiee sous verrou (R2). */
class PaymentTest extends BookingTestSupport {

	private final String year = String.valueOf(Dates.today().getYear());

	@Test
	void theCustomerAcceptsTheQuoteAndOpensThePaymentPage() throws Exception {
		String lea = client();
		long id = quoted(lea, EVENT, EVENT.plusDays(2), tents, 2);

		postAs(lea, "/api/me/bookings/{id}/checkout", "{}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://checkout.stripe.com/c/pay/cs_test_1"));

		var request = ArgumentCaptor.forClass(PaymentGateway.CheckoutRequest.class);
		verify(gateway).checkout(request.capture());
		assertThat(request.getValue().amountCents()).isEqualTo(18000);
		assertThat(request.getValue().successUrl()).isEqualTo("http://localhost:5173/mon-espace/reservations/" + id + "?paiement=ok");
		// Rien n'est confirme tant que Stripe n'a pas confirme le paiement (R8)
		getAs(lea, "/api/me/bookings/{id}", id)
				.andExpect(jsonPath("$.status").value("DEVIS"))
				.andExpect(jsonPath("$.payments[0].statusLabel").value("En attente"))
				.andExpect(jsonPath("$.paidCents").value(0));
	}

	@Test
	void noPaymentBeforeTheQuoteOrAfterItExpires() throws Exception {
		String lea = client();
		long demande = idOf(postAs(lea, "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, tents, 1)));
		postAs(lea, "/api/me/bookings/{id}/checkout", "{}", demande)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ce devis n'attend plus de paiement."));

		long id = quoted(lea, EVENT, EVENT, tents, 1);
		entityManager.flush();
		jdbc.update("update booking set quote_valid_until = ? where id = ?", Dates.today().minusDays(1), id);
		entityManager.clear();
		postAs(lea, "/api/me/bookings/{id}/checkout", "{}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ce devis a expiré : contactez-nous pour le renouveler."));
		verify(gateway, never()).checkout(any());
	}

	@Test
	void theSignedWebhookConfirmsTheBookingAndIssuesTheDepositInvoice() throws Exception {
		String lea = client();
		long id = quoted(lea, EVENT, EVENT.plusDays(2), tents, 2);
		String session = checkout(lea, id);

		webhook(session).andExpect(status().isOk());
		// Stripe peut renvoyer le meme message : il n'est compte qu'une fois
		webhook(session).andExpect(status().isOk());

		getAs(lea, "/api/me/bookings/{id}", id)
				.andExpect(jsonPath("$.status").value("CONFIRMEE"))
				.andExpect(jsonPath("$.paidCents").value(18000))
				.andExpect(jsonPath("$.balanceCents").value(42000))
				.andExpect(jsonPath("$.payments[0].methodLabel").value("Carte bancaire"))
				.andExpect(jsonPath("$.payments[0].statusLabel").value("Payé"))
				.andExpect(jsonPath("$.invoices.length()").value(1))
				.andExpect(jsonPath("$.invoices[0].number").value("FAC-" + year + "-00001"))
				.andExpect(jsonPath("$.invoices[0].kindLabel").value("Facture d'acompte"))
				.andExpect(jsonPath("$.invoices[0].amountCents").value(18000))
				.andExpect(jsonPath("$.events[2].note").value("Acompte de 180,00\u00a0€ payé par carte"));
		// Le materiel est maintenant bloque
		postAs(null, "/api/quote/estimate", request(EVENT, EVENT, "RETRAIT", null, false, tents, 3))
				.andExpect(jsonPath("$.problems[0]").value("Barnum 3 x 6 m : 2 disponible(s) seulement à ces dates."));
	}

	@Test
	void badlySignedMessagesAreRejected() throws Exception {
		String lea = client();
		long id = quoted(lea, EVENT, EVENT, tents, 1);
		String session = checkout(lea, id);
		String payload = """
				{"type": "checkout.session.completed", "data": {"object": {"id": "%s", "payment_intent": "pi_x", "payment_status": "paid"}}}"""
				.formatted(session);
		long now = Instant.now().getEpochSecond();

		mockMvc.perform(post("/api/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content(payload)
				.header("Stripe-Signature", "t=" + now + ",v1=" + StripeSignature.sign(now + "." + payload, "whsec_pirate")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content(payload))
				.andExpect(status().isBadRequest());
		// Signature correcte mais message trop ancien (rejeu)
		long old = now - 3600;
		mockMvc.perform(post("/api/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content(payload)
				.header("Stripe-Signature", "t=" + old + ",v1=" + StripeSignature.sign(old + "." + payload, WEBHOOK_SECRET)))
				.andExpect(status().isBadRequest());

		getAs(lea, "/api/me/bookings/{id}", id).andExpect(jsonPath("$.status").value("DEVIS"));
	}

	@Test
	void equipmentBookedDuringPaymentIsRefunded() throws Exception {
		String lea = client();
		long id = quoted(lea, EVENT, EVENT, tents, 3);
		String session = checkout(lea, id);
		// Un autre client confirme 2 barnums pendant que Lea est sur la page de paiement
		confirmedBooking(EVENT, EVENT, tents, 2);

		webhook(session).andExpect(status().isOk());

		verify(gateway).refund("pi_" + session, 13500);
		getAs(lea, "/api/me/bookings/{id}", id)
				.andExpect(jsonPath("$.status").value("DEVIS"))
				.andExpect(jsonPath("$.payments[0].statusLabel").value("Remboursé"))
				.andExpect(jsonPath("$.paidCents").value(0))
				.andExpect(jsonPath("$.invoices.length()").value(0))
				.andExpect(jsonPath("$.events[2].note")
						.value("Paiement de 135,00\u00a0€ remboursé : Barnum 3 x 6 m : 2 disponible(s) seulement à ces dates."));
	}

	@Test
	void offlinePaymentsDepositThenBalance() throws Exception {
		String lea = client();
		long id = quoted(lea, EVENT, EVENT.plusDays(2), tents, 2);
		String commercial = tokenFor(Role.COMMERCIAL);

		postAs(commercial, "/api/bookings/{id}/payments", "{\"method\": \"VIREMENT\", \"amountCents\": 10000}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.amountCents").value("Il faut au moins l'acompte (180,00\u00a0€) pour confirmer la réservation."));
		postAs(commercial, "/api/bookings/{id}/payments", "{\"method\": \"CARTE\", \"amountCents\": 18000}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.method").value("Les paiements par carte passent par la page de paiement en ligne."));

		postAs(commercial, "/api/bookings/{id}/payments", "{\"method\": \"VIREMENT\", \"amountCents\": 18000, \"note\": \"Virement du 15/10\"}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMEE"))
				.andExpect(jsonPath("$.events[2].note").value("Acompte reçu : 180,00\u00a0€ (virement)"))
				.andExpect(jsonPath("$.payments[0].author").value("Test COMMERCIAL"))
				.andExpect(jsonPath("$.invoices[0].kind").value("ACOMPTE"));

		postAs(commercial, "/api/bookings/{id}/payments", "{\"method\": \"CHEQUE\", \"amountCents\": 50000}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.amountCents").value("Le montant dépasse le reste à payer (420,00\u00a0€)."));
		// Le chauffeur encaisse le solde en especes a la livraison
		postAs(tokenFor(Role.LOGISTIQUE), "/api/bookings/{id}/payments", "{\"method\": \"ESPECES\", \"amountCents\": 42000}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balanceCents").value(0))
				.andExpect(jsonPath("$.invoices[1].number").value("FAC-" + year + "-00002"))
				.andExpect(jsonPath("$.invoices[1].kindLabel").value("Facture de solde"));
		postAs(manager, "/api/bookings/{id}/payments", "{\"method\": \"ESPECES\", \"amountCents\": 100}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Cette réservation est déjà entièrement payée."));
		postAs(lea, "/api/bookings/{id}/payments", "{\"method\": \"ESPECES\", \"amountCents\": 100}", id).andExpect(status().isForbidden());
	}

	@Test
	void invoiceNumbersFollowEachOtherWithoutGaps() throws Exception {
		paidByCard(client(), EVENT, EVENT, tents, 1);
		paidByCard(client(), EVENT, EVENT, chairs, 10);
		long third = paidByCard(client(), EVENT, EVENT, tents, 1);

		getAs(manager, "/api/bookings/{id}", third).andExpect(jsonPath("$.invoices[0].number").value("FAC-" + year + "-00003"));
		verify(gateway, never()).refund(anyString(), anyInt());
	}
}
