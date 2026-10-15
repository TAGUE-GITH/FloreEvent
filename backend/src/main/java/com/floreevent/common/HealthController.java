package com.floreevent.common;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sante du service, appelee par l'hebergeur : l'API repond et la base de donnees est joignable. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

	private final JdbcTemplate jdbc;

	public HealthController(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@GetMapping
	public Map<String, String> health() {
		jdbc.queryForObject("SELECT 1", Integer.class);
		return Map.of("status", "ok");
	}
}
