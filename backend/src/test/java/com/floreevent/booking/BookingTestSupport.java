package com.floreevent.booking;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;
import com.floreevent.IntegrationTest;
import com.floreevent.common.Dates;
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

	@BeforeEach
	void setUpCatalog() throws Exception {
		manager = tokenFor(Role.GERANT);
		long category = idOf(postAs(manager, "/api/categories", "{\"name\": \"Location\"}"));
		tents = item(category, "BARNUM", "Barnum 3 x 6 m", 15000, 120000, 8000, 4);
		chairs = item(category, "CHAISE", "Chaise Napoléon", 350, 4500, 0, 120);
		// Fontainebleau : environ 19 km de Melun par la route (zone 2 : 70 €)
		when(geocoder.locate(anyString())).thenReturn(Optional.of(new Geocoder.Location("5 Rue Grande 77300 Fontainebleau", 48.4047, 2.7016)));
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
}
