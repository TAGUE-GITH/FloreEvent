package com.floreevent.payment;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.BookingService;
import com.floreevent.booking.Payment;
import com.floreevent.booking.PaymentService;
import com.floreevent.user.User;

/**
 * Page de paiement de demonstration, active seulement sans cle Stripe (app.payments.simulation=true). Le paiement
 * simule suit exactement le meme chemin que la confirmation signee de Stripe.
 */
@RestController
@RequestMapping("/api/simulated-payments")
@PreAuthorize("hasRole('CLIENT')")
@Transactional
public class SimulatedPaymentController {

	public record SimulatedSession(Long bookingId, String reference, int amountCents) {

		static SimulatedSession of(Payment payment) {
			return new SimulatedSession(payment.getBooking().getId(), payment.getBooking().getReference(), payment.getAmountCents());
		}
	}

	private final PaymentService payments;
	private final BookingService bookings;

	public SimulatedPaymentController(PaymentService payments, BookingService bookings) {
		this.payments = payments;
		this.bookings = bookings;
	}

	@GetMapping("/{session}")
	public SimulatedSession get(@PathVariable String session, @AuthenticationPrincipal User user) {
		return SimulatedSession.of(payments.simulatedSession(session, bookings.customerIdOf(user)));
	}

	@PostMapping("/{session}/pay")
	public SimulatedSession pay(@PathVariable String session, @AuthenticationPrincipal User user) {
		return SimulatedSession.of(payments.paySimulated(session, bookings.customerIdOf(user)));
	}
}
