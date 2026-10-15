package com.floreevent.user;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.floreevent.IntegrationTest;

/** Espace personnel de chaque membre de l'equipe : informations, mot de passe, photo de profil. */
class ProfileTest extends IntegrationTest {

	@Test
	void everyoneUpdatesTheirProfileAndPassword() throws Exception {
		createUser(Role.LOGISTIQUE, "paul@floreevent.test");
		String token = tokenFor(users.findByEmailIgnoreCase("paul@floreevent.test").orElseThrow());

		patchAs(token, "/api/me", "{\"firstName\": \"Paul\", \"lastName\": \"Martin\", \"phone\": \"0612345678\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Paul Martin"));
		postAs(token, "/api/me/password", "{\"currentPassword\": \"faux\", \"newPassword\": \"nouveau-mot-de-passe\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Le mot de passe actuel est incorrect."));
		postAs(token, "/api/me/password", "{\"currentPassword\": \"%s\", \"newPassword\": \"nouveau-mot-de-passe\"}".formatted(PASSWORD))
				.andExpect(status().isNoContent());
		postAs(null, "/api/auth/login", "{\"email\": \"paul@floreevent.test\", \"password\": \"nouveau-mot-de-passe\"}")
				.andExpect(status().isOk());
	}

	@Test
	void everyoneHasAProfilePhotoServedPublicly() throws Exception {
		String token = tokenFor(Role.COMMERCIAL);

		String avatar = JsonPath.read(putAs(token, "/api/me/avatar", "{\"dataUrl\": \"%s\"}".formatted(photo("png")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.avatarUrl", startsWith("/api/images/")))
				.andReturn().getResponse().getContentAsString(), "$.avatarUrl");

		getAs(null, avatar).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
		deleteAs(token, "/api/me/avatar").andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").doesNotExist());
		getAs(null, avatar).andExpect(status().isNotFound());
	}

	@Test
	void onlyRealImagesAreAccepted() throws Exception {
		String fake = "data:image/png;base64," + Base64.getEncoder().encodeToString("pas une image".getBytes());

		putAs(tokenFor(Role.COMMERCIAL), "/api/me/avatar", "{\"dataUrl\": \"%s\"}".formatted(fake))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ce fichier n'est pas une image valide (JPEG ou PNG)."));
	}
}
