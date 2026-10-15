package com.floreevent.contact;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

	List<ContactMessage> findAllByOrderByHandledAscCreatedAtDesc();

	long countByHandledFalse();
}
