package com.floreevent.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.floreevent.IntegrationTest;
import com.floreevent.user.Role;

/** US-04 : materiel en reparation, retire de la location pour une periode (regle R1). */
class MaintenanceTest extends IntegrationTest {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Paris"));

	private String manager;
	private String logistics;
	private long chairs;

	@BeforeEach
	void setUp() throws Exception {
		manager = tokenFor(Role.GERANT);
		logistics = tokenFor(Role.LOGISTIQUE);
		long category = idOf(postAs(manager, "/api/categories", "{\"name\": \"Mobilier\"}"));
		chairs = idOf(postAs(manager, "/api/items", """
				{"reference": "CHAISE", "name": "Chaise Napoléon", "categoryId": %d, "dayPriceCents": 350,
				 "replacementValueCents": 4500, "quantity": 10, "active": true}""".formatted(category)));
	}

	private String repair(int quantity, LocalDate from, LocalDate to) {
		return "{\"quantity\": %d, \"fromDate\": \"%s\", \"toDate\": %s, \"reason\": \"Pieds tordus\"}"
				.formatted(quantity, from, to == null ? "null" : "\"" + to + "\"");
	}

	@Test
	void theLogisticsTeamPutsDamagedItemsInRepair() throws Exception {
		postAs(logistics, "/api/items/{id}/maintenance", repair(2, TODAY, null), chairs)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.quantity").value(2))
				.andExpect(jsonPath("$.reason").value("Pieds tordus"));

		getAs(manager, "/api/items/{id}", chairs).andExpect(jsonPath("$.inRepair").value(2));
		getAs(manager, "/api/maintenance").andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].itemName").value("Chaise Napoléon"));
		postAs(tokenFor(Role.COMMERCIAL), "/api/items/{id}/maintenance", repair(1, TODAY, null), chairs)
				.andExpect(status().isForbidden());
	}

	@Test
	void neverMoreInRepairThanInStock() throws Exception {
		postAs(logistics, "/api/items/{id}/maintenance", repair(2, TODAY, null), chairs);

		postAs(logistics, "/api/items/{id}/maintenance", repair(9, TODAY.plusDays(3), TODAY.plusDays(5)), chairs)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("8 exemplaire(s) au plus peuvent encore être mis en réparation sur cette période."));
		// Une periode qui ne chevauche pas la premiere reparation n'est pas concernee
		postAs(logistics, "/api/items/{id}/maintenance", repair(10, TODAY.minusDays(10), TODAY.minusDays(8)), chairs)
				.andExpect(status().isCreated());
		postAs(logistics, "/api/items/{id}/maintenance", repair(1, TODAY.plusDays(5), TODAY.plusDays(2)), chairs)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.toDate").value("La date de fin doit être après la date de début."));
	}

	@Test
	void aRepairedItemIsAvailableAgainFromToday() throws Exception {
		long id = idOf(postAs(logistics, "/api/items/{id}/maintenance", repair(3, TODAY.minusDays(4), null), chairs));

		postAs(logistics, "/api/maintenance/{id}/end", "{}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.toDate").value(TODAY.minusDays(1).toString()));
		getAs(manager, "/api/items/{id}", chairs).andExpect(jsonPath("$.inRepair").value(0));
		getAs(manager, "/api/maintenance").andExpect(jsonPath("$", hasSize(0)));
		getAs(manager, "/api/items/{id}/maintenance", chairs).andExpect(jsonPath("$", hasSize(1)));
	}
}
