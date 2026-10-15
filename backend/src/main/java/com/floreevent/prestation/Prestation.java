package com.floreevent.prestation;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** US-18 : prestation proposee avec le materiel (decoration florale, installation lumiere...), prix TTC « a partir de ». */
@Entity
public class Prestation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	private String description;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Prestation() {
	}

	public Prestation(String name, String description, int priceCents, boolean active) {
		update(name, description, priceCents, active);
	}

	public void update(String name, String description, int priceCents, boolean active) {
		this.name = name;
		this.description = description;
		this.priceCents = priceCents;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getPriceCents() {
		return priceCents;
	}

	public boolean isActive() {
		return active;
	}
}
