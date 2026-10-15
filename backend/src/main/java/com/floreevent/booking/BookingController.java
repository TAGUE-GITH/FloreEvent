package com.floreevent.booking;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.BookingDtos.BookingResponse;
import com.floreevent.booking.BookingDtos.BookingSummary;
import com.floreevent.booking.BookingDtos.CancelRequest;
import com.floreevent.booking.BookingDtos.DeliverRequest;
import com.floreevent.booking.BookingDtos.OfflinePaymentRequest;
import com.floreevent.booking.BookingDtos.QuoteRequest;
import com.floreevent.booking.BookingDtos.ReturnRequest;
import com.floreevent.user.Role;
import com.floreevent.user.User;

import jakarta.validation.Valid;

/** Demandes et reservations traitees par l'equipe : devis (US-09), paiements hors ligne (US-12), annulation (US-13). */
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize(Role.STAFF)
@Transactional
public class BookingController {

	/** Devis, remises et annulations : le gerant et les commerciaux. */
	private static final String SALES = "hasAnyRole('GERANT', 'COMMERCIAL')";

	private final BookingService bookings;
	private final PaymentService payments;
	private final CancellationService cancellations;
	private final BookingFiles files;
	private final LogisticsService logistics;

	public BookingController(BookingService bookings, PaymentService payments, CancellationService cancellations, BookingFiles files,
			LogisticsService logistics) {
		this.logistics = logistics;
		this.bookings = bookings;
		this.payments = payments;
		this.cancellations = cancellations;
		this.files = files;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<BookingSummary> list() {
		return bookings.all().stream().map(BookingSummary::of).toList();
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	public BookingResponse get(@PathVariable Long id) {
		return BookingResponse.of(bookings.get(id), true);
	}

	@PostMapping("/{id}/quote")
	@PreAuthorize(SALES)
	public BookingResponse sendQuote(@PathVariable Long id, @Valid @RequestBody QuoteRequest request, @AuthenticationPrincipal User user) {
		return BookingResponse.of(bookings.sendQuote(id, request, user), true);
	}

	@GetMapping("/{id}/cancellation")
	@Transactional(readOnly = true)
	public CancellationService.Preview cancellation(@PathVariable Long id) {
		return cancellations.preview(bookings.get(id));
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize(SALES)
	public BookingResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request, @AuthenticationPrincipal User user) {
		return BookingResponse.of(bookings.cancelByTeam(id, request.reason(), user), true);
	}

	@PostMapping("/{id}/payments")
	public BookingResponse recordPayment(@PathVariable Long id, @Valid @RequestBody OfflinePaymentRequest request,
			@AuthenticationPrincipal User user) {
		Booking booking = bookings.get(id);
		payments.recordOffline(booking, request, bookings.account(user));
		return BookingResponse.of(booking, true);
	}

	/** US-16 : materiel livre ou remis, caution recue. */
	@PostMapping("/{id}/deliver")
	public BookingResponse deliver(@PathVariable Long id, @Valid @RequestBody DeliverRequest request, @AuthenticationPrincipal User user) {
		return BookingResponse.of(logistics.deliver(bookings.get(id), request.securityDepositMethod(), bookings.account(user)), true);
	}

	/** US-17 : etat des lieux au retour. */
	@PostMapping("/{id}/return")
	public BookingResponse recordReturn(@PathVariable Long id, @Valid @RequestBody ReturnRequest request, @AuthenticationPrincipal User user) {
		return BookingResponse.of(logistics.recordReturn(bookings.get(id), request, bookings.account(user)), true);
	}

	@PostMapping("/{id}/close")
	public BookingResponse close(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return BookingResponse.of(logistics.close(bookings.get(id), bookings.account(user)), true);
	}

	@GetMapping("/{id}/documents/{kind}")
	@Transactional(readOnly = true)
	public ResponseEntity<byte[]> document(@PathVariable Long id, @PathVariable String kind) {
		return files.document(bookings.get(id), kind);
	}

	@GetMapping("/{id}/invoices/{invoiceId}")
	@Transactional(readOnly = true)
	public ResponseEntity<byte[]> invoice(@PathVariable Long id, @PathVariable Long invoiceId) {
		return files.invoice(bookings.get(id), invoiceId);
	}
}
