package com.floreevent.booking;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.floreevent.user.Role;

/** US-05 : disponibilite par dates (regle R1), dans le catalogue public. */
class AvailabilityTest extends BookingTestSupport {

	private void availableIs(LocalDate from, LocalDate to, long item, int expected) throws Exception {
		getAs(null, "/api/catalog/items/{id}?from={from}&to={to}", item, from, to).andExpect(jsonPath("$.available").value(expected));
	}

	@Test
	void visitorsSeeTheActiveItemsWithoutInternalData() throws Exception {
		long old = item(idOf(postAs(manager, "/api/categories", "{\"name\": \"Archives\"}")), "VIEUX", "Ancien modèle", 1000, 1000, 0, 2);
		entityManager.flush();
		jdbc.update("update item set active = false where id = ?", old);
		entityManager.clear();

		getAs(null, "/api/catalog/items")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name").value("Barnum 3 x 6 m"))
				.andExpect(jsonPath("$[0].dayPriceCents").value(15000))
				.andExpect(jsonPath("$[0].setupPriceCents").value(8000))
				.andExpect(jsonPath("$[0].replacementValueCents").doesNotExist())
				.andExpect(jsonPath("$[0].reference").doesNotExist())
				.andExpect(jsonPath("$[0].available").doesNotExist());
		getAs(null, "/api/catalog/items/{id}", old).andExpect(status().isNotFound());
		getAs(null, "/api/catalog/categories").andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void aConfirmedBookingBlocksItsDatesPlusTheEveAndTheDayAfter() throws Exception {
		confirmedBooking(EVENT, EVENT.plusDays(2), chairs, 30);

		availableIs(EVENT, EVENT.plusDays(2), chairs, 90);
		availableIs(EVENT.plusDays(3), EVENT.plusDays(3), chairs, 90);   // lendemain : nettoyage
		availableIs(EVENT.minusDays(1), EVENT.minusDays(1), chairs, 90); // veille : preparation
		availableIs(EVENT.plusDays(4), EVENT.plusDays(6), chairs, 120);
		availableIs(EVENT.minusDays(5), EVENT.minusDays(2), chairs, 120);
		getAs(null, "/api/catalog/items?from={from}&to={to}", EVENT, EVENT)
				.andExpect(jsonPath("$[?(@.name == 'Chaise Napoléon')].available").value(90));
	}

	@Test
	void requestsDoNotBlockButRepairsDo() throws Exception {
		postAs(client(), "/api/me/bookings", request(EVENT, EVENT, "RETRAIT", null, false, tents, 3)).andExpect(status().isCreated());
		postAs(tokenFor(Role.LOGISTIQUE), "/api/items/{id}/maintenance", """
				{"quantity": 1, "fromDate": "%s", "toDate": "%s"}""".formatted(EVENT.minusDays(3), EVENT), tents);

		availableIs(EVENT, EVENT, tents, 3);
		availableIs(EVENT.plusDays(1), EVENT.plusDays(1), tents, 4);
	}

	@Test
	void theDatesAreChecked() throws Exception {
		getAs(null, "/api/catalog/items?from={from}&to={to}", EVENT, EVENT.minusDays(1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La date de fin doit être après la date de début."));
	}
}
