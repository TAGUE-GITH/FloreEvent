package com.floreevent.booking;

import java.time.Instant;

import com.floreevent.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Une etape de la reservation : qui l'a faite et quand (R4). */
@Entity
@Table(name = "booking_event")
public class BookingEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id")
	private Booking booking;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status;

	private String note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private User author;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected BookingEvent() {
	}

	BookingEvent(Booking booking, BookingStatus status, String note, User author) {
		this.booking = booking;
		this.status = status;
		this.note = note;
		this.author = author;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public String getNote() {
		return note;
	}

	public User getAuthor() {
		return author;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
