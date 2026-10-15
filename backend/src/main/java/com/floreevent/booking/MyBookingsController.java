package com.floreevent.booking;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.BookingDtos.BookingRequest;
import com.floreevent.booking.BookingDtos.BookingResponse;
import com.floreevent.booking.BookingDtos.BookingSummary;
import com.floreevent.user.User;

import jakarta.validation.Valid;

/** US-07 et US-08 : demandes de devis et reservations du client connecte. */
@RestController
@RequestMapping("/api/me/bookings")
@PreAuthorize("hasRole('CLIENT')")
@Transactional
public class MyBookingsController {

	private final BookingService bookings;

	public MyBookingsController(BookingService bookings) {
		this.bookings = bookings;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingResponse request(@Valid @RequestBody BookingRequest request, @AuthenticationPrincipal User user) {
		return BookingResponse.of(bookings.request(request, user), false);
	}

	@GetMapping
	public List<BookingSummary> list(@AuthenticationPrincipal User user) {
		return bookings.mine(user).stream().map(BookingSummary::of).toList();
	}

	@GetMapping("/{id}")
	public BookingResponse get(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return BookingResponse.of(bookings.mine(id, user), false);
	}

	@PostMapping("/{id}/cancel")
	public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return BookingResponse.of(bookings.cancelByCustomer(id, user), false);
	}
}
