package com.floreevent.contact;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Message envoye depuis la page contact du site (US-21). */
@Entity
@Table(name = "contact_message")
public class ContactMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	private String phone;

	@Column(name = "event_date")
	private LocalDate eventDate;

	@Column(nullable = false)
	private String message;

	@Column(nullable = false)
	private boolean handled;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected ContactMessage() {
	}

	public ContactMessage(String name, String email, String phone, LocalDate eventDate, String message) {
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.eventDate = eventDate;
		this.message = message;
	}

	public void markHandled() {
		handled = true;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public LocalDate getEventDate() {
		return eventDate;
	}

	public String getMessage() {
		return message;
	}

	public boolean isHandled() {
		return handled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
