package com.floreevent.customer;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Fiche client : particulier ou entreprise qui loue du materiel. */
@Entity
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	/** Societe ou association, pour les evenements professionnels (facultatif). */
	private String company;

	@Column(nullable = false)
	private String phone;

	private String email;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Customer() {
	}

	public Customer(String fullName, String company, String phone, String email) {
		this.fullName = fullName;
		this.company = company;
		this.phone = phone;
		this.email = email;
	}

	public void update(String fullName, String company, String phone) {
		this.fullName = fullName;
		this.company = company;
		this.phone = phone;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getCompany() {
		return company;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
