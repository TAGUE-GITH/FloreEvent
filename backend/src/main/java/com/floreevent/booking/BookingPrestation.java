package com.floreevent.booking;

import com.floreevent.prestation.Prestation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Prestation demandee avec une reservation : nom et prix figes (R3). */
@Entity
@Table(name = "booking_prestation")
public class BookingPrestation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prestation_id")
	private Prestation prestation;

	@Column(nullable = false)
	private String name;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	protected BookingPrestation() {
	}

	BookingPrestation(Booking booking, Prestation prestation) {
		this.booking = booking;
		this.prestation = prestation;
		this.name = prestation.getName();
		this.priceCents = prestation.getPriceCents();
	}

	public Long getPrestationId() {
		return prestation.getId();
	}

	public String getName() {
		return name;
	}

	public int getPriceCents() {
		return priceCents;
	}
}
