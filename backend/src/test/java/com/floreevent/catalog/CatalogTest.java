package com.floreevent.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.floreevent.IntegrationTest;
import com.floreevent.user.Role;

/** US-03 : catalogue de location (categories, articles, tarifs en centimes, photos). */
class CatalogTest extends IntegrationTest {

	private String manager;
	private long tents;

	@BeforeEach
	void setUp() throws Exception {
		manager = tokenFor(Role.GERANT);
		tents = idOf(postAs(manager, "/api/categories", "{\"name\": \"Tentes et barnums\"}"));
	}

	private static String item(String reference, String name, long category, int dayPrice, int quantity) {
		return """
				{"reference": "%s", "name": "%s", "categoryId": %d, "description": "Structure aluminium, bâche blanche.",
				 "dayPriceCents": %d, "replacementValueCents": 120000, "setupPriceCents": 8000, "quantity": %d, "active": true}"""
				.formatted(reference, name, category, dayPrice, quantity);
	}

	@Test
	void theManagerAddsAnItemToTheCatalog() throws Exception {
		postAs(manager, "/api/items", item(" barnum-3x6 ", "Barnum 3 x 6 m", tents, 15000, 4))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reference").value("BARNUM-3X6"))
				.andExpect(jsonPath("$.category.name").value("Tentes et barnums"))
				.andExpect(jsonPath("$.dayPriceCents").value(15000))
				.andExpect(jsonPath("$.replacementValueCents").value(120000))
				.andExpect(jsonPath("$.setupPriceCents").value(8000))
				.andExpect(jsonPath("$.quantity").value(4))
				.andExpect(jsonPath("$.inRepair").value(0))
				.andExpect(jsonPath("$.images", hasSize(0)));
	}

	@Test
	void theTeamReadsTheCatalogButOnlyTheManagerChangesIt() throws Exception {
		postAs(manager, "/api/items", item("BARNUM-3X6", "Barnum 3 x 6 m", tents, 15000, 4));

		for (Role role : new Role[] { Role.COMMERCIAL, Role.LOGISTIQUE }) {
			String token = tokenFor(role);
			getAs(token, "/api/items").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
			postAs(token, "/api/items", item("CHAISE", "Chaise", tents, 300, 10)).andExpect(status().isForbidden());
			postAs(token, "/api/categories", "{\"name\": \"Mobilier\"}").andExpect(status().isForbidden());
		}
		getAs(null, "/api/items").andExpect(status().isUnauthorized());
	}

	@Test
	void pricesAndQuantitiesAreChecked() throws Exception {
		postAs(manager, "/api/items", item("BARNUM", "Barnum", tents, 0, -1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.dayPriceCents").value("Le prix par jour doit être supérieur à 0."))
				.andExpect(jsonPath("$.errors.quantity").value("La quantité ne peut pas être négative."));
		postAs(manager, "/api/items", item("BARNUM", "Barnum", tents, 15000, 2)).andExpect(status().isCreated());
		postAs(manager, "/api/items", item("barnum", "Autre barnum", tents, 9000, 1))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Un article existe déjà avec la référence BARNUM."));
	}

	@Test
	void theCatalogIsSearchedByNameAndCategory() throws Exception {
		long furniture = idOf(postAs(manager, "/api/categories", "{\"name\": \"Mobilier\"}"));
		postAs(manager, "/api/items", item("BARNUM", "Barnum 3 x 6 m", tents, 15000, 4));
		postAs(manager, "/api/items", item("CHAISE-NAP", "Chaise Napoléon blanche", furniture, 350, 120));
		long table = idOf(postAs(manager, "/api/items", item("TABLE-RONDE", "Table ronde 10 personnes", furniture, 1200, 20)));
		putAs(manager, "/api/items/{id}", item("TABLE-RONDE", "Table ronde 10 personnes", furniture, 1200, 20).replace("\"active\": true", "\"active\": false"), table)
				.andExpect(jsonPath("$.active").value(false));

		getAs(manager, "/api/items?q=chaise").andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].reference").value("CHAISE-NAP"));
		getAs(manager, "/api/items?categoryId={id}", furniture).andExpect(jsonPath("$", hasSize(2)));
		getAs(manager, "/api/categories").andExpect(jsonPath("$[0].name").value("Mobilier")).andExpect(jsonPath("$[0].itemCount").value(2));
	}

	@Test
	void aCategoryWithItemsCannotBeDeleted() throws Exception {
		postAs(manager, "/api/items", item("BARNUM", "Barnum", tents, 15000, 4));
		long empty = idOf(postAs(manager, "/api/categories", "{\"name\": \"Vide\"}"));

		deleteAs(manager, "/api/categories/{id}", tents)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Cette catégorie contient des articles : déplacez-les avant de la supprimer."));
		deleteAs(manager, "/api/categories/{id}", empty).andExpect(status().isNoContent());
		postAs(manager, "/api/categories", "{\"name\": \"tentes et barnums\"}").andExpect(status().isConflict());
	}

	@Test
	void anItemHasSeveralPhotosAndAMainOne() throws Exception {
		long id = idOf(postAs(manager, "/api/items", item("BARNUM", "Barnum", tents, 15000, 4)));
		String body = "{\"dataUrl\": \"%s\"}";

		postAs(manager, "/api/items/{id}/images", body.formatted(photo("jpg")), id).andExpect(status().isOk());
		String second = JsonPath.read(postAs(manager, "/api/items/{id}/images", body.formatted(photo("png")), id)
				.andExpect(jsonPath("$.images", hasSize(2)))
				.andReturn().getResponse().getContentAsString(), "$.images[1].id");

		postAs(manager, "/api/items/{id}/images/{image}/main", "{}", id, second)
				.andExpect(jsonPath("$.images[0].id").value(second))
				.andExpect(jsonPath("$.imageUrl", startsWith("/api/images/" + second)));
		deleteAs(manager, "/api/items/{id}/images/{image}", id, second).andExpect(jsonPath("$.images", hasSize(1)));
		getAs(null, "/api/images/" + second).andExpect(status().isNotFound());

		postAs(tokenFor(Role.COMMERCIAL), "/api/items/{id}/images", body.formatted(photo("jpg")), id)
				.andExpect(status().isForbidden());
		postAs(manager, "/api/items/{id}/images", body.formatted("data:image/png;base64,"
				+ Base64.getEncoder().encodeToString("pas une image".getBytes())), id).andExpect(status().isBadRequest());
	}
}
