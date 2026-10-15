package com.floreevent.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.floreevent.IntegrationTest;
import com.floreevent.user.Role;
import com.floreevent.user.User;

/** US-01 : connexion des employes. */
class AuthTest extends IntegrationTest {

	private static final String LOGIN = "/api/auth/login";

	private String credentials(String email, String password) {
		return """
				{"email": "%s", "password": "%s"}""".formatted(email, password);
	}

	@Test
	void anEmployeeLogsInAndReceivesAToken() throws Exception {
		createUser(Role.COMMERCIAL, "awa@floreevent.test");

		postAs(null, LOGIN, credentials("Awa@FloreEvent.test", PASSWORD))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token", not(emptyString())))
				.andExpect(jsonPath("$.user.email").value("awa@floreevent.test"))
				.andExpect(jsonPath("$.user.role").value("COMMERCIAL"))
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	@Test
	void wrongCredentialsAreRefusedWithAClearMessage() throws Exception {
		createUser(Role.COMMERCIAL, "awa@floreevent.test");

		postAs(null, LOGIN, credentials("awa@floreevent.test", "mauvais-mot-de-passe"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
		postAs(null, LOGIN, credentials("inconnu@floreevent.test", PASSWORD))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void aDeactivatedAccountCannotLogInNorUseItsToken() throws Exception {
		User user = createUser(Role.LOGISTIQUE, "paul@floreevent.test");
		String token = tokenFor(user);
		user.setActive(false);
		users.save(user);

		postAs(null, LOGIN, credentials("paul@floreevent.test", PASSWORD))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Ce compte a été désactivé. Contactez le gérant."));
		getAs(token, "/api/auth/me").andExpect(status().isUnauthorized());
	}

	@Test
	void theConnectedEmployeeSeesTheirProfile() throws Exception {
		User user = createUser(Role.COMMERCIAL, "eric@floreevent.test");

		getAs(tokenFor(user), "/api/auth/me")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("eric@floreevent.test"))
				.andExpect(jsonPath("$.role").value("COMMERCIAL"));
		getAs(null, "/api/auth/me").andExpect(status().isUnauthorized());
		getAs("jeton-invalide", "/api/auth/me").andExpect(status().isUnauthorized());
	}

	@Test
	void emailAndPasswordAreRequired() throws Exception {
		postAs(null, LOGIN, "{\"email\": \"\", \"password\": \"\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}
}
