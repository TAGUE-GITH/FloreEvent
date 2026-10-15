package com.floreevent.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.floreevent.IntegrationTest;
import com.floreevent.common.Dates;
import com.floreevent.payment.PaymentGateway;
import com.floreevent.payment.StripeSignature;
import com.floreevent.quote.Geocoder;
import com.floreevent.user.Role;

import jakarta.persistence.EntityManager;

/** Catalogue de test commun : un barnum (4) et des chaises (120), un client inscrit. */
abstract class BookingTestSupport extends IntegrationTest {

	/** Un samedi dans un mois : la reservation commence apres demain. */
	protected static final LocalDate EVENT = Dates.today().plusDays(30);

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected EntityManager entityManager;

	protected String manager;
	protected long tents;
	protected long chairs;
	private int clients;
	private int sessions;

	@BeforeEach
	void setUpCatalog() throws Exception {
		manager = tokenFor(Role.GERANT);
		long category = idOf(postAs(manager, "/api/categories", "{\"name\": \"Location\"}"));
		tents = item(category, "BARNUM", "Barnum 3 x 6 m", 15000, 120000, 8000, 4);
		chairs = item(category, "CHAISE", "Chaise Napoléon", 350, 4500, 0, 120);
		// Fontainebleau : environ 19 km de Melun par la route (zone 2 : 70 €)
		when(geocoder.locate(anyString())).thenReturn(Optional.of(new Geocoder.Location("5 Rue Grande 77300 Fontainebleau", 48.4047, 2.7016)));
		// Chaque ouverture de la page de paiement cree une nouvelle session Stripe
		when(gateway.checkout(any())).thenAnswer(call -> {
			sessions++;
			return new PaymentGateway.Checkout("cs_test_" + sessions, "https://checkout.stripe.com/c/pay/cs_test_" + sessions);
		});
	}

	protected long item(long category, String reference, String name, int dayPrice, int replacement, int setup, int quantity) throws Exception {
		return idOf(postAs(manager, "/api/items", """
				{"reference": "%s", "name": "%s", "categoryId": %d, "dayPriceCents": %d, "replacementValueCents": %d,
				 "setupPriceCents": %d, "quantity": %d, "active": true}""".formatted(reference, name, category, dayPrice, replacement, setup, quantity)));
	}

	/** Nouveau client inscrit depuis le site : renvoie son jeton. */
	protected String client() throws Exception {
		clients++;
		String body = postAs(null, "/api/auth/register", """
				{"firstName": "Léa", "lastName": "Moreau", "email": "lea%d@client.test", "phone": "06 12 34 56 78",
				 "password": "%s", "acceptTerms": true}""".formatted(clients, PASSWORD)).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	/** Demande de devis : dates, mode, adresse, installation, puis (article, quantite). */
	protected static String request(LocalDate start, LocalDate end, String mode, String address, boolean installation, long... itemAndQuantity) {
		StringBuilder lines = new StringBuilder();
		for (int i = 0; i < itemAndQuantity.length; i += 2)
			lines.append(i == 0 ? "" : ",").append("{\"itemId\": %d, \"quantity\": %d}".formatted(itemAndQuantity[i], itemAndQuantity[i + 1]));
		return """
				{"startDate": "%s", "endDate": "%s", "deliveryMode": "%s", "address": %s, "installation": %s,
				 "note": "Mariage en extérieur", "lines": [%s]}"""
				.formatted(start, end, mode, address == null ? "null" : "\"" + address + "\"", installation, lines);
	}

	/** Une demande confirmee bloque le materiel (le paiement de l'acompte arrive au sprint 3). */
	protected long confirmedBooking(LocalDate start, LocalDate end, long item, int quantity) throws Exception {
		long id = idOf(postAs(client(), "/api/me/bookings", request(start, end, "RETRAIT", null, false, item, quantity)));
		entityManager.flush();
		jdbc.update("update booking set status = 'CONFIRMEE' where id = ?", id);
		entityManager.clear();
		return id;
	}

	/** Demande du client (retrait), puis devis envoye par le gerant sans remise. */
	protected long quoted(String client, LocalDate start, LocalDate end, long item, int quantity) throws Exception {
		long id = idOf(postAs(client, "/api/me/bookings", request(start, end, "RETRAIT", null, false, item, quantity)));
		postAs(manager, "/api/bookings/{id}/quote", "{}", id).andExpect(status().isOk());
		return id;
	}

	/** Le client accepte le devis et ouvre la page de paiement : identifiant de la session Stripe. */
	protected String checkout(String client, long id) throws Exception {
		String url = JsonPath.read(body(postAs(client, "/api/me/bookings/{id}/checkout", "{}", id).andExpect(status().isOk())), "$.url");
		return url.substring(url.lastIndexOf('/') + 1);
	}

	/** Stripe previent que la session est payee : message signe avec le secret du webhook. */
	protected ResultActions webhook(String sessionId) throws Exception {
		String payload = """
				{"id": "evt_%s", "type": "checkout.session.completed",
				 "data": {"object": {"id": "%s", "payment_intent": "pi_%s", "payment_status": "paid"}}}""".formatted(sessionId, sessionId, sessionId);
		long timestamp = Instant.now().getEpochSecond();
		return mockMvc.perform(post("/api/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content(payload)
				.header("Stripe-Signature", "t=" + timestamp + ",v1=" + StripeSignature.sign(timestamp + "." + payload, WEBHOOK_SECRET)));
	}

	/** Reservation payee par carte : devis, page de paiement, confirmation de Stripe. */
	protected long paidByCard(String client, LocalDate start, LocalDate end, long item, int quantity) throws Exception {
		long id = quoted(client, start, end, item, quantity);
		webhook(checkout(client, id)).andExpect(status().isOk());
		return id;
	}

	protected static String body(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString();
	}
}
