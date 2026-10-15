package com.floreevent.booking;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.BookingDtos.BookingResponse;
import com.floreevent.booking.BookingDtos.BookingSummary;
import com.floreevent.user.Role;

/** Demandes et reservations vues par l'equipe (leur traitement arrive au sprint 3). */
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize(Role.STAFF)
@Transactional(readOnly = true)
public class BookingController {

	private final BookingService bookings;

	public BookingController(BookingService bookings) {
		this.bookings = bookings;
	}

	@GetMapping
	public List<BookingSummary> list() {
		return bookings.all().stream().map(BookingSummary::of).toList();
	}

	@GetMapping("/{id}")
	public BookingResponse get(@PathVariable Long id) {
		return BookingResponse.of(bookings.get(id), true);
	}
}
