package com.floreevent.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.user.Role;

/** Indique aux pages de connexion si le site est une demonstration, et avec quels comptes s'y connecter. */
@RestController
public class DemoController {

	public record Account(String email, Role role, String name) {
	}

	public record DemoInfo(boolean enabled, String password, List<Account> accounts) {
	}

	private final boolean enabled;

	public DemoController(@Value("${app.demo.enabled}") boolean enabled) {
		this.enabled = enabled;
	}

	@GetMapping("/api/demo")
	public DemoInfo info() {
		if (!enabled) return new DemoInfo(false, null, List.of());
		return new DemoInfo(true, DemoService.PASSWORD, DemoService.ACCOUNTS.stream()
				.map(a -> new Account(a.email(), a.role(), a.firstName() + " " + a.lastName())).toList());
	}
}
