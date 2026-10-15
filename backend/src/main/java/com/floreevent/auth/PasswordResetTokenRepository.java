package com.floreevent.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	/** Liens encore non utilises d'un compte (annules quand un nouveau lien est demande). */
	List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);
}
