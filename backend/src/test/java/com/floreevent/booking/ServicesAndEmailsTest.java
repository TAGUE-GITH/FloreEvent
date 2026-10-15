package com.floreevent.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.floreevent.common.Dates;
import com.floreevent.user.Role;

/** US-18 prestations, US-19 emails, US-20 tableau de bord. */
class ServicesAndEmailsTest extends BookingTestSupport {

	@Autowired
	private ReminderJob reminders;

	private long prestation(String name, int price, boolean active) throws Exception {
		return idOf(postAs(manager, "/api/prestations", """
				{"name": "%s", "description": "Sur mesure", "priceCents": %d, "active": %s}""".formatted(name, price, active))
				.andExpect(status().isCreated()));
	}

	@Test
	void theManagerPublishesPrestationsThatCustomersAddToTheirQuote() throws Exception {
		long flowers = prestation("Décoration florale", 45000, true);
		prestation("Feu d'artifice", 90000, false);
		postAs(tokenFor(Role.COMMERCIAL), "/api/prestations", "{\"name\": \"DJ\", \"priceCents\": 100}").andExpect(status().isForbidden());
		postAs(manager, "/api/prestations", "{\"name\": \"décoration florale\", \"priceCents\": 100}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").value("Cette prestation existe déjà."));

		getAs(null, "/api/catalog/prestations")
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Décoration florale"));

		// 1 barnum 1 jour (150 €) + decoration florale (450 €) = 600 € ; acompte 180 €
		String withFlowers = request(EVENT, EVENT, "RETRAIT", null, false, tents, 1).replace("\"lines\"", "\"prestationIds\": [" + flowers + "], \"lines\"");
		postAs(null, "/api/quote/estimate", withFlowers)
				.andExpect(jsonPath("$.prestationsCents").value(45000))
				.andExpect(jsonPath("$.totalCents").value(60000))
				.andExpect(jsonPath("$.prestations[0].name").value("Décoration florale"));
		long id = idOf(postAs(client(), "/api/me/bookings", withFlowers));
		// Le prix est fige : un changement de tarif ne modifie pas la demande (R3)
		putAs(manager, "/api/prestations/{id}", "{\"name\": \"Décoration florale\", \"priceCents\": 99000, \"active\": true}", flowers);
		postAs(manager, "/api/bookings/{id}/quote", "{}", id)
				.andExpect(jsonPath("$.prestations[0].priceCents").value(45000))
				.andExpect(jsonPath("$.totalCents").value(60000))
				.andExpect(jsonPath("$.depositCents").value(18000));
	}

	@Test
	void theCustomerIsEmailedWhenTheQuoteIsSentAndWhenTheBookingIsConfirmed() throws Exception {
		String lea = client();
		long id = quoted(lea, EVENT, EVENT, tents, 1);
		verify(emails).send(eq("lea1@client.test"), startsWith("Votre devis FloreEvent RES-"), eq("Bonjour Léa Moreau,"), anyList(),
				eq("Voir mon devis"), eq("http://localhost:5173/mon-espace/reservations/" + id), anyString());
		webhook(checkout(lea, id));
		verify(emails).send(eq("lea1@client.test"), startsWith("Réservation confirmée RES-"), anyString(), anyList(), anyString(), anyString(),
				any());
	}

	@Test
	void customersAreRemindedTwoDaysBeforeTheirEvent() throws Exception {
		paidByCard(client(), Dates.today().plusDays(2), Dates.today().plusDays(2), tents, 1);
		paidByCard(client(), Dates.today().plusDays(3), Dates.today().plusDays(3), tents, 1);
		quoted(client(), Dates.today().plusDays(2), Dates.today().plusDays(2), chairs, 5);

		assertThat(reminders.sendReminders()).isEqualTo(1);
		verify(emails).send(eq("lea1@client.test"), startsWith("Votre événement approche"), anyString(), anyList(), anyString(), anyString(),
				anyString());
		verify(emails, never()).send(eq("lea2@client.test"), startsWith("Votre événement approche"), anyString(), anyList(), anyString(),
				anyString(), anyString());
	}

	@Test
	void theDashboardSummarizesTheActivity() throws Exception {
		paidByCard(client(), EVENT, EVENT.plusDays(2), tents, 2);
		idOf(postAs(client(), "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, chairs, 30)));
		quoted(client(), EVENT, EVENT, chairs, 10);

		getAs(tokenFor(Role.LOGISTIQUE), "/api/dashboard")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.revenueThisMonthCents").value(18000))
				.andExpect(jsonPath("$.revenueByMonth.length()").value(6))
				.andExpect(jsonPath("$.revenueByMonth[5].cents").value(18000))
				.andExpect(jsonPath("$.pendingRequests").value(1))
				.andExpect(jsonPath("$.quotesAwaiting").value(1))
				.andExpect(jsonPath("$.upcomingBookings").value(1))
				.andExpect(jsonPath("$.balanceDueCents").value(42000))
				.andExpect(jsonPath("$.topItems[0].name").value("Barnum 3 x 6 m"))
				.andExpect(jsonPath("$.topItems[0].quantity").value(2))
				.andExpect(jsonPath("$.nextBookings[0].customer").value("Léa Moreau"));
		getAs(client(), "/api/dashboard").andExpect(status().isForbidden());
	}
}
