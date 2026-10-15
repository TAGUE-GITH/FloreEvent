package com.floreevent.booking;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.floreevent.booking.BookingDtos.CheckoutResponse;
import com.floreevent.user.User;

import jakarta.validation.Valid;

/** US-07, US-08, US-10, US-11, US-13 : demandes, paiement de l'acompte, documents et annulation du client connecte. */
@RestController
@RequestMapping("/api/me/bookings")
@PreAuthorize("hasRole('CLIENT')")
@Transactional
public class MyBookingsController {

	private final BookingService bookings;
	private final PaymentService payments;
	private final CancellationService cancellations;
	private final BookingFiles files;

	public MyBookingsController(BookingService bookings, PaymentService payments, CancellationService cancellations, BookingFiles files) {
		this.bookings = bookings;
		this.payments = payments;
		this.cancellations = cancellations;
		this.files = files;
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

	/** Le client accepte le devis : adresse de la page de paiement de l'acompte. */
	@PostMapping("/{id}/checkout")
	public CheckoutResponse checkout(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return new CheckoutResponse(payments.checkout(bookings.mine(id, user)).url());
	}

	/** Ce que couterait l'annulation aujourd'hui. */
	@GetMapping("/{id}/cancellation")
	public CancellationService.Preview cancellation(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return cancellations.preview(bookings.mine(id, user));
	}

	@PostMapping("/{id}/cancel")
	public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return BookingResponse.of(bookings.cancelByCustomer(id, user), false);
	}

	@GetMapping("/{id}/documents/{kind}")
	public ResponseEntity<byte[]> document(@PathVariable Long id, @PathVariable String kind, @AuthenticationPrincipal User user) {
		return files.document(bookings.mine(id, user), kind);
	}

	@GetMapping("/{id}/invoices/{invoiceId}")
	public ResponseEntity<byte[]> invoice(@PathVariable Long id, @PathVariable Long invoiceId, @AuthenticationPrincipal User user) {
		return files.invoice(bookings.mine(id, user), invoiceId);
	}
}
