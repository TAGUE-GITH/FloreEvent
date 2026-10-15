package com.floreevent.booking;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.floreevent.quote.Geocoder;
import com.floreevent.user.Role;

/** US-06 a US-08 : compte client, estimation en ligne et demande de devis. */
class BookingRequestTest extends BookingTestSupport {

	@Test
	void anyVisitorGetsTheDetailedPriceForTheirSelection() throws Exception {
		// 2 barnums sur 3 jours avec installation, 100 chaises, livres a Fontainebleau (zone 2)
		postAs(null, "/api/quote/estimate", request(EVENT, EVENT.plusDays(2), "LIVRAISON", "5 rue Grande Fontainebleau", true, tents, 2, chairs, 100))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days").value(3))
				.andExpect(jsonPath("$.lines[0].lineTotalCents").value(60000))
				.andExpect(jsonPath("$.lines[1].lineTotalCents").value(70000))
				.andExpect(jsonPath("$.rentalCents").value(130000))
				.andExpect(jsonPath("$.setupCents").value(16000))
				.andExpect(jsonPath("$.distanceKm").value(19))
				.andExpect(jsonPath("$.deliveryCents").value(7000))
				.andExpect(jsonPath("$.totalCents").value(153000))
				.andExpect(jsonPath("$.vatCents").value(25500))
				.andExpect(jsonPath("$.depositCents").value(45900))
				// Caution : 30 % de (2 x 1 200 € + 100 x 45 €) = 2 070 €
				.andExpect(jsonPath("$.securityDepositCents").value(207000))
				.andExpect(jsonPath("$.address").value("5 Rue Grande 77300 Fontainebleau"))
				.andExpect(jsonPath("$.bookable").value(true));
	}

	@Test
	void theEstimateWarnsWhenAnItemIsShort() throws Exception {
		confirmedBooking(EVENT, EVENT, tents, 3);

		postAs(null, "/api/quote/estimate", request(EVENT, EVENT, "RETRAIT", null, false, tents, 2))
				.andExpect(jsonPath("$.bookable").value(false))
				.andExpect(jsonPath("$.lines[0].available").value(1))
				.andExpect(jsonPath("$.problems[0]").value("Barnum 3 x 6 m : 1 disponible(s) seulement à ces dates."));
	}

	@Test
	void theDeliveryAddressIsChecked() throws Exception {
		when(geocoder.locate(anyString())).thenReturn(Optional.empty());
		postAs(null, "/api/quote/estimate", request(EVENT, EVENT, "LIVRAISON", "nulle part", false, chairs, 10))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.address").value("Adresse introuvable : vérifiez le numéro, la rue et la ville."));

		when(geocoder.locate(anyString())).thenReturn(Optional.of(new Geocoder.Location("Place du Capitole 31000 Toulouse", 43.6045, 1.4440)));
		postAs(null, "/api/quote/estimate", request(EVENT, EVENT, "LIVRAISON", "Toulouse", false, chairs, 10))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.address").value("Au-delà de 50 km, contactez-nous pour un devis personnalisé."));

		postAs(null, "/api/quote/estimate", request(EVENT, EVENT, "RETRAIT", null, true, tents, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.installation").value("L’installation est proposée avec la livraison."));
		postAs(null, "/api/quote/estimate", request(EVENT, EVENT.minusDays(1), "RETRAIT", null, false, tents, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.endDate").value("La date de fin doit être après la date de début."));
		postAs(null, "/api/quote/estimate", request(EVENT.minusDays(30), EVENT.minusDays(30), "RETRAIT", null, false, tents, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.startDate").value("La location doit commencer au plus tôt demain."));
	}

	@Test
	void aCustomerAccountRequiresAcceptingTheTerms() throws Exception {
		postAs(null, "/api/auth/register", """
				{"firstName": "Léa", "lastName": "Moreau", "email": "lea@client.test", "phone": "0612345678", "password": "%s"}"""
				.formatted(PASSWORD))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.acceptTerms").value("Vous devez accepter les conditions générales et la politique de confidentialité."));
		String client = client();
		getAs(client, "/api/auth/me").andExpect(jsonPath("$.role").value("CLIENT"));
		getAs(client, "/api/items").andExpect(status().isForbidden());
	}

	@Test
	void aCustomerSendsAQuoteRequestWithFrozenPrices() throws Exception {
		String client = client();

		long id = idOf(postAs(client, "/api/me/bookings", request(EVENT, EVENT.plusDays(2), "LIVRAISON", "5 rue Grande Fontainebleau", true, tents, 2, chairs, 100))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reference", matchesPattern("RES-\\d{4}-\\d{5}")))
				.andExpect(jsonPath("$.status").value("DEMANDE"))
				.andExpect(jsonPath("$.totalCents").value(153000))
				.andExpect(jsonPath("$.note").value("Mariage en extérieur"))
				.andExpect(jsonPath("$.lines", hasSize(2))));

		// Les prix sont figes (R3) : un changement de tarif ne modifie pas la demande
		putAs(manager, "/api/items/{id}", """
				{"reference": "BARNUM", "name": "Barnum 3 x 6 m", "categoryId": %d, "dayPriceCents": 99900, "replacementValueCents": 120000,
				 "setupPriceCents": 8000, "quantity": 4, "active": true}""".formatted(jdbc.queryForObject("select category_id from item where id = ?", Long.class, tents)), tents);
		getAs(client, "/api/me/bookings/{id}", id)
				.andExpect(jsonPath("$.lines[0].dayPriceCents").value(15000))
				.andExpect(jsonPath("$.totalCents").value(153000));
		getAs(client, "/api/me/bookings").andExpect(jsonPath("$", hasSize(1)));
		getAs(tokenFor(Role.COMMERCIAL), "/api/bookings").andExpect(jsonPath("$[0].customer").value("Léa Moreau"));
	}

	@Test
	void aRequestIsRefusedWhenAnItemIsNoLongerAvailable() throws Exception {
		confirmedBooking(EVENT, EVENT, tents, 4);

		postAs(client(), "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, tents, 1))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Barnum 3 x 6 m : 0 disponible(s) seulement à ces dates."));
	}

	@Test
	void eachCustomerSeesAndCancelsOnlyTheirOwnRequests() throws Exception {
		String lea = client();
		long id = idOf(postAs(lea, "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, chairs, 10)));

		getAs(client(), "/api/me/bookings/{id}", id).andExpect(status().isNotFound());
		postAs(null, "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, chairs, 10)).andExpect(status().isUnauthorized());
		postAs(manager, "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, chairs, 10)).andExpect(status().isForbidden());
		postAs(lea, "/api/me/bookings/{id}/cancel", "{}", id).andExpect(jsonPath("$.status").value("ANNULEE"));
	}
}
