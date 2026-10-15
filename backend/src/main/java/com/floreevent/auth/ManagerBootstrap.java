package com.floreevent.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.floreevent.user.Role;
import com.floreevent.user.User;
import com.floreevent.user.UserRepository;

/**
 * Compte du proprietaire (gerant), cree au demarrage s'il n'existe pas encore (meme si d'autres comptes existent). Un compte existant n'est jamais modifie : le mot de passe change ensuite dans le profil est garde.
 * En production, l'email et le mot de passe sont saisis par le gerant dans les variables d'environnement
 * de l'hebergeur (BOOTSTRAP_EMAIL, BOOTSTRAP_PASSWORD) : ils n'apparaissent jamais dans le code.
 */
@Component
public class ManagerBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManagerBootstrap.class);

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;

	public ManagerBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap.email:}") String email, @Value("${app.bootstrap.password:}") String password) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank() || password.isBlank()) return;
		String address = email.trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(address)) return;
		users.save(new User(address, passwordEncoder.encode(password), "Gérant", "FloreEvent", null, Role.GERANT));
		log.info("Compte gérant du propriétaire créé : {}", address);
	}
}
