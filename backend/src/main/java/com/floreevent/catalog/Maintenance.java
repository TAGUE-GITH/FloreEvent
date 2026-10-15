package com.floreevent.catalog;

import java.time.Instant;
import java.time.LocalDate;

import com.floreevent.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Exemplaires retires de la location du from_date au to_date inclus (to_date vide : jusqu'a nouvel ordre). */
@Entity
public class Maintenance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id")
	private Item item;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "from_date", nullable = false)
	private LocalDate fromDate;

	@Column(name = "to_date")
	private LocalDate toDate;

	private String reason;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id")
	private User author;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Maintenance() {
	}

	public Maintenance(Item item, int quantity, LocalDate fromDate, LocalDate toDate, String reason, User author) {
		this.item = item;
		this.quantity = quantity;
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.reason = reason;
		this.author = author;
	}

	/** L'article est repare : il redevient disponible a partir de ce jour. */
	void endBefore(LocalDate day) {
		toDate = day.minusDays(1);
	}

	public Long getId() {
		return id;
	}

	public Item getItem() {
		return item;
	}

	public int getQuantity() {
		return quantity;
	}

	public LocalDate getFromDate() {
		return fromDate;
	}

	public LocalDate getToDate() {
		return toDate;
	}

	public String getReason() {
		return reason;
	}

	public User getAuthor() {
		return author;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
