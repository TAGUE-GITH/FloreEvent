package com.floreevent.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.floreevent.IntegrationTest;
import com.floreevent.user.Role;

/** Compte du proprietaire (gerant) cree au demarrage depuis BOOTSTRAP_EMAIL et BOOTSTRAP_PASSWORD. */
class ManagerBootstrapTest extends IntegrationTest {

	@Autowired
	private PasswordEncoder passwordEncoder;

	private ManagerBootstrap bootstrap(String email, String password) {
		return new ManagerBootstrap(users, passwordEncoder, email, password);
	}

	@Test
	void theOwnerAccountIsCreatedEvenWhenTheDemoAlreadyHasAccounts() throws Exception {
		createUser(Role.GERANT, "autre-gerant@floreevent.test");

		bootstrap("Proprietaire@Gmail.com", "mot-de-passe-du-proprietaire").run(null);

		postAs(null, "/api/auth/login", "{\"email\": \"proprietaire@gmail.com\", \"password\": \"mot-de-passe-du-proprietaire\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.role").value("GERANT"));
	}

	@Test
	void anExistingAccountIsNeverChanged() {
		var owner = createUser(Role.GERANT, "proprietaire@gmail.com");
		String hash = owner.getPasswordHash();

		// Le proprietaire a change son mot de passe dans son profil : un redemarrage ne doit pas l'ecraser
		bootstrap("proprietaire@gmail.com", "ancien-mot-de-passe-render").run(null);

		assertThat(users.findByEmailIgnoreCase("proprietaire@gmail.com").orElseThrow().getPasswordHash()).isEqualTo(hash);
		assertThat(users.count()).isEqualTo(1);
	}

	@Test
	void nothingIsCreatedWithoutEmailOrPassword() {
		bootstrap("", "mot-de-passe").run(null);
		bootstrap("proprietaire@gmail.com", " ").run(null);

		assertThat(users.findByEmailIgnoreCase("proprietaire@gmail.com")).isEmpty();
	}
}
