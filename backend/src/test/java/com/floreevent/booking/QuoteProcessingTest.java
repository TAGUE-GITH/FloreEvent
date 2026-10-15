package com.floreevent.booking;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.floreevent.common.Dates;
import com.floreevent.user.Role;

/** US-09 : l'equipe transforme une demande en devis (remise, message) ou n'y donne pas suite. */
class QuoteProcessingTest extends BookingTestSupport {

	@Test
	void theSalesTeamSendsTheQuoteWithADiscountAndAMessage() throws Exception {
		String lea = client();
		long id = idOf(postAs(lea, "/api/me/bookings", request(EVENT, EVENT.plusDays(2), "RETRAIT", null, false, tents, 2)));
		String commercial = tokenFor(Role.COMMERCIAL);

		// 2 barnums x 3 jours = 600 € ; remise de 50 € : 550 €, acompte 30 % = 165 €
		postAs(commercial, "/api/bookings/{id}/quote", """
				{"discountCents": 5000, "message": "  Remise fidélité, montage possible le vendredi.  "}""", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DEVIS"))
				.andExpect(jsonPath("$.discountCents").value(5000))
				.andExpect(jsonPath("$.totalCents").value(55000))
				.andExpect(jsonPath("$.depositCents").value(16500))
				.andExpect(jsonPath("$.quoteValidUntil").value(Dates.today().plusDays(7).toString()))
				.andExpect(jsonPath("$.events[1].note").value("Devis envoyé au client"))
				.andExpect(jsonPath("$.events[1].author").value("Test COMMERCIAL"));

		getAs(lea, "/api/me/bookings/{id}", id)
				.andExpect(jsonPath("$.statusLabel").value("Devis envoyé"))
				.andExpect(jsonPath("$.quoteMessage").value("Remise fidélité, montage possible le vendredi."))
				.andExpect(jsonPath("$.quoteExpired").value(false))
				.andExpect(jsonPath("$.balanceCents").value(55000));

		// Le devis peut etre modifie tant qu'il n'est pas accepte
		postAs(commercial, "/api/bookings/{id}/quote", "{\"discountCents\": 0}", id)
				.andExpect(jsonPath("$.totalCents").value(60000))
				.andExpect(jsonPath("$.depositCents").value(18000))
				.andExpect(jsonPath("$.events[2].note").value("Devis modifié"));
	}

	@Test
	void excessiveDiscountsAndOtherRolesAreRefused() throws Exception {
		String lea = client();
		long id = idOf(postAs(lea, "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, tents, 1)));

		postAs(manager, "/api/bookings/{id}/quote", "{\"discountCents\": 15000}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.discountCents").value("La remise doit rester inférieure au montant du devis."));
		postAs(manager, "/api/bookings/{id}/quote", "{\"discountCents\": -100}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.discountCents").value("La remise ne peut pas être négative."));
		postAs(tokenFor(Role.LOGISTIQUE), "/api/bookings/{id}/quote", "{}", id).andExpect(status().isForbidden());
		postAs(lea, "/api/bookings/{id}/quote", "{}", id).andExpect(status().isForbidden());
	}

	@Test
	void noQuoteWhenTheEquipmentWasBookedInTheMeantime() throws Exception {
		long id = idOf(postAs(client(), "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, tents, 3)));
		confirmedBooking(EVENT, EVENT, tents, 2);

		postAs(manager, "/api/bookings/{id}/quote", "{}", id)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Barnum 3 x 6 m : 2 disponible(s) seulement à ces dates. Adaptez la demande avec le client."));
	}

	@Test
	void theTeamDeclinesARequestWithAReason() throws Exception {
		String lea = client();
		long id = idOf(postAs(lea, "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, tents, 1)));

		postAs(manager, "/api/bookings/{id}/cancel", "{\"reason\": \" \"}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.reason").value("Indiquez le motif de l'annulation."));
		postAs(manager, "/api/bookings/{id}/cancel", "{\"reason\": \"Date déjà complète\"}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ANNULEE"));
		getAs(lea, "/api/me/bookings/{id}", id)
				.andExpect(jsonPath("$.events[1].note").value("Annulée par FloreEvent : Date déjà complète"))
				.andExpect(jsonPath("$.balanceCents").value(0));
		postAs(manager, "/api/bookings/{id}/quote", "{}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ce devis ne peut plus être modifié."));
	}
}
