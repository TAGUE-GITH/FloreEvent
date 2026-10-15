package com.floreevent.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Jetons JWT signes : l'email de l'employe, valable quelques heures. */
@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(String email) {
		Date now = new Date();
		return Jwts.builder()
				.subject(email)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	/** Email contenu dans le jeton, ou null si le jeton est invalide ou expire. */
	public String emailOf(String token) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
