package com.floreevent.booking;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	/** Verrou : deux notifications Stripe simultanees pour la meme session ne comptent le paiement qu'une fois. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.checkoutSessionId = :sessionId")
	Optional<Payment> findBySessionForUpdate(@Param("sessionId") String sessionId);

	Optional<Payment> findByCheckoutSessionId(String sessionId);
}
