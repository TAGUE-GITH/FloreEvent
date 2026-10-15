package com.floreevent.auth;

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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import com.floreevent.IntegrationTest;
import com.floreevent.demo.DemoPurge;
import com.floreevent.demo.DemoService;
import com.floreevent.quote.Geocoder;
import com.floreevent.user.Role;
import com.jayway.jsonpath.JsonPath;

/** US-21 contact, US-22 mot de passe oublie, US-23 demonstration. */
class ShowcaseAndDemoTest extends IntegrationTest {

	@Autowired
	private DemoService demo;

	@Autowired
	private DemoPurge purge;

	@Autowired
	private jakarta.persistence.EntityManager entityManager;

	@Test
	void aForgottenPasswordIsResetWithASingleUseLink() throws Exception {
		createUser(Role.COMMERCIAL, "julien@floreevent.test");

		postAs(null, "/api/auth/forgot-password", "{\"email\": \"inconnu@floreevent.test\"}").andExpect(status().isNoContent());
		verify(emails, never()).send(anyString(), anyString(), anyString(), anyList(), anyString(), anyString(), anyString());

		postAs(null, "/api/auth/forgot-password", "{\"email\": \" Julien@floreevent.test \"}").andExpect(status().isNoContent());
		var link = ArgumentCaptor.forClass(String.class);
		verify(emails).send(eq("julien@floreevent.test"), eq("Réinitialiser votre mot de passe FloreEvent"), anyString(), anyList(),
				eq("Choisir un nouveau mot de passe"), link.capture(), anyString());
		assertThat(link.getValue()).startsWith("http://localhost:5173/reinitialiser?token=");
		String token = link.getValue().substring(link.getValue().indexOf('=') + 1);

		postAs(null, "/api/auth/reset-password", "{\"token\": \"%s\", \"password\": \"nouveau-mot-de-passe\"}".formatted(token))
				.andExpect(status().isNoContent());
		postAs(null, "/api/auth/login", "{\"email\": \"julien@floreevent.test\", \"password\": \"nouveau-mot-de-passe\"}")
				.andExpect(status().isOk());
		postAs(null, "/api/auth/reset-password", "{\"token\": \"%s\", \"password\": \"encore-un-autre\"}".formatted(token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ce lien n'est plus valable. Demandez-en un nouveau."));
	}

	@Test
	void visitorsWriteToTheTeamFromTheContactPage() throws Exception {
		postAs(null, "/api/contact", "{\"name\": \"Élodie\", \"email\": \"pas-un-email\", \"message\": \"\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").value("Cet email n'est pas valide."))
				.andExpect(jsonPath("$.errors.message").value("Écrivez votre message."));
		postAs(null, "/api/contact", """
				{"name": "Élodie Garnier", "email": "elodie@exemple.fr", "eventDate": "2027-06-12", "message": "Mariage de 120 personnes"}""")
				.andExpect(status().isCreated());
		verify(emails).send(eq("contact@floreevent.fr"), eq("Nouveau message de Élodie Garnier"), anyString(), anyList(), any(), any(), any());

		String sales = tokenFor(Role.COMMERCIAL);
		long id = ((Number) JsonPath.read(getAs(sales, "/api/contact-messages").andReturn().getResponse().getContentAsString(), "$[0].id"))
				.longValue();
		getAs(sales, "/api/contact-messages").andExpect(jsonPath("$[0].eventDate").value("2027-06-12"));
		postAs(sales, "/api/contact-messages/{id}/handled", "{}", id).andExpect(jsonPath("$.handled").value(true));
		getAs(tokenFor(Role.LOGISTIQUE), "/api/contact-messages").andExpect(status().isForbidden());
	}

	@Test
	void theDemoIsLoadedOnceWithAnAccountPerRole() throws Exception {
		org.mockito.Mockito.when(geocoder.locate(anyString()))
				.thenReturn(Optional.of(new Geocoder.Location("12 Rue Saint-Barthélemy 77000 Melun", 48.5397, 2.6553)));
		assertThat(demo.seed(false)).isTrue();
		assertThat(demo.seed(false)).isFalse();

		String token = JsonPath.read(postAs(null, "/api/auth/login", "{\"email\": \"gerant@demo.floreevent.fr\", \"password\": \"%s\"}"
				.formatted(DemoService.PASSWORD)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
		getAs(token, "/api/bookings").andExpect(jsonPath("$.length()").value(12));
		getAs(token, "/api/dashboard")
				.andExpect(jsonPath("$.pendingRequests").value(2))
				.andExpect(jsonPath("$.quotesAwaiting").value(2))
				.andExpect(jsonPath("$.depositsHeld").value(1));
		getAs(null, "/api/catalog/prestations").andExpect(jsonPath("$.length()").value(4));
		// Les comptes de demonstration servent a tous les visiteurs : on ne peut pas les verrouiller
		postAs(token, "/api/me/password", "{\"currentPassword\": \"%s\", \"newPassword\": \"pirate-2026\"}".formatted(DemoService.PASSWORD))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Le mot de passe des comptes de démonstration ne peut pas être changé."));
		verify(emails, never()).send(startsWith("x"), anyString(), anyString(), anyList(), anyString(), anyString(), anyString());
		// Demo desactivee dans les tests : les pages de connexion n'affichent pas de comptes
		getAs(null, "/api/demo").andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	void switchingToRealModeRemovesTheDemoButKeepsRealData() throws Exception {
		org.mockito.Mockito.when(geocoder.locate(anyString()))
				.thenReturn(Optional.of(new Geocoder.Location("12 Rue Saint-Barthélemy 77000 Melun", 48.5397, 2.6553)));
		String owner = tokenFor(createUser(Role.GERANT, "proprietaire@floreevent.fr"));
		long category = ((Number) JsonPath.read(postAs(owner, "/api/categories", "{\"name\": \"Vaisselle\"}").andReturn().getResponse()
				.getContentAsString(), "$.id")).longValue();
		postAs(owner, "/api/items", """
				{"reference": "PLAT-OVALE", "name": "Plat ovale", "categoryId": %d, "dayPriceCents": 300, "replacementValueCents": 2000,
				 "quantity": 10, "active": true}""".formatted(category)).andExpect(status().isCreated());
		postAs(null, "/api/contact", "{\"name\": \"Vrai client\", \"email\": \"vrai@client.fr\", \"message\": \"Bonjour\"}");
		demo.seed(false);

		entityManager.flush();
		assertThat(purge.purge()).isTrue();
		assertThat(purge.purge()).isFalse();
		entityManager.clear();

		postAs(null, "/api/auth/login", "{\"email\": \"gerant@demo.floreevent.fr\", \"password\": \"%s\"}".formatted(DemoService.PASSWORD))
				.andExpect(status().isUnauthorized());
		getAs(null, "/api/catalog/items").andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("Plat ovale"));
		getAs(null, "/api/catalog/prestations").andExpect(jsonPath("$.length()").value(0));
		getAs(owner, "/api/bookings").andExpect(jsonPath("$.length()").value(0));
		getAs(owner, "/api/contact-messages").andExpect(jsonPath("$.length()").value(1));
		getAs(owner, "/api/categories").andExpect(jsonPath("$.length()").value(1));
		getAs(owner, "/api/users").andExpect(jsonPath("$[?(@.email =~ /.*demo.*/)]").isEmpty());
	}
}
