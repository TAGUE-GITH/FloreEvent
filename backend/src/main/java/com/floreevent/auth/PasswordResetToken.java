package com.floreevent.auth;

import java.time.Instant;

import com.floreevent.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Lien « mot de passe oublie » : a usage unique, limite dans le temps, seule son empreinte est stockee. */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected PasswordResetToken() {
	}

	public PasswordResetToken(String tokenHash, User user, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.user = user;
		this.expiresAt = expiresAt;
	}

	public boolean isUsable() {
		return usedAt == null && Instant.now().isBefore(expiresAt);
	}

	public void markUsed() {
		usedAt = Instant.now();
	}

	public User getUser() {
		return user;
	}
}
