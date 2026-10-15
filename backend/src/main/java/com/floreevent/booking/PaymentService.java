package com.floreevent.booking;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.booking.BookingDtos.OfflinePaymentRequest;
import com.floreevent.common.ApiException;
import com.floreevent.common.Money;
import com.floreevent.payment.PaymentGateway;
import com.floreevent.user.User;

/** US-10 et US-12 : acompte par carte (Stripe), paiements hors ligne, factures. */
@Service
public class PaymentService {

	private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
	private static final List<BookingStatus> AFTER_CONFIRMATION = List.of(BookingStatus.CONFIRMEE, BookingStatus.EN_COURS,
			BookingStatus.RETOURNEE);

	private final PaymentRepository payments;
	private final AvailabilityService availability;
	private final InvoiceService invoices;
	private final PaymentGateway gateway;
	private final String frontendUrl;

	public PaymentService(PaymentRepository payments, AvailabilityService availability, InvoiceService invoices,
			PaymentGateway gateway, @Value("${app.frontend-url}") String frontendUrl) {
		this.payments = payments;
		this.availability = availability;
		this.invoices = invoices;
		this.gateway = gateway;
		this.frontendUrl = frontendUrl;
	}

	/**
	 * Le client accepte le devis : session de paiement de l'acompte. La reservation n'est confirmee qu'a la reception
	 * de la confirmation signee (R8).
	 */
	@Transactional
	public PaymentGateway.Checkout checkout(Booking booking) {
		if (booking.getStatus() != BookingStatus.DEVIS) throw ApiException.badRequest("Ce devis n'attend plus de paiement.");
		if (booking.isQuoteExpired()) throw ApiException.badRequest("Ce devis a expiré : contactez-nous pour le renouveler.");
		String shortage = availability.shortage(booking, false);
		if (shortage != null) throw ApiException.conflict(shortage + " Contactez-nous pour adapter le devis.");
		String page = frontendUrl + "/mon-espace/reservations/" + booking.getId();
		var checkout = gateway.checkout(new PaymentGateway.CheckoutRequest(booking.getId(), booking.getReference(),
				booking.getCustomer().getEmail(), booking.getDepositCents(), page + "?paiement=ok", page + "?paiement=annule"));
		payments.save(Payment.checkout(booking, booking.getDepositCents(), checkout.sessionId()));
		return checkout;
	}

	/**
	 * Session payee (webhook signe de Stripe, ou paiement simule). Sans effet si elle a deja ete traitee. La disponibilite
	 * est reverifiee sous verrou (R2) : si le materiel a ete reserve entre-temps, le client est rembourse.
	 */
	@Transactional
	public void completeCheckout(String sessionId, String paymentIntentId) {
		Payment payment = payments.findBySessionForUpdate(sessionId).orElse(null);
		if (payment == null) {
			LOG.warn("Paiement inconnu pour la session {}", sessionId);
			return;
		}
		if (payment.getStatus() != PaymentStatus.EN_ATTENTE) return;
		payment.markPaid(paymentIntentId);
		Booking booking = payment.getBooking();
		String amount = Money.euros(payment.getAmountCents());

		if (booking.getStatus() == BookingStatus.DEVIS) {
			String shortage = availability.shortage(booking, true);
			if (shortage == null) {
				booking.confirm("Acompte de " + amount + " payé par carte", null);
				invoices.forPayment(payment);
			} else refund(payment, booking, "Paiement de " + amount + " remboursé : " + shortage);
		} else if (AFTER_CONFIRMATION.contains(booking.getStatus()) && booking.getBalanceCents() >= 0) {
			booking.addNote("Paiement de " + amount + " reçu par carte", null);
			invoices.forPayment(payment);
		} else refund(payment, booking, "Paiement de " + amount + " remboursé : la réservation n'attendait plus ce paiement.");
	}

	private void refund(Payment payment, Booking booking, String note) {
		gateway.refund(payment.getPaymentIntentId(), payment.getAmountCents());
		payment.refund(payment.getAmountCents());
		booking.addNote(note, null);
	}

	/** US-12 : virement, cheque ou especes. Un acompte recu confirme la reservation. */
	@Transactional
	public void recordOffline(Booking booking, OfflinePaymentRequest request, User author) {
		if (request.method() == PaymentMethod.CARTE)
			throw ApiException.invalidField("method", "Les paiements par carte passent par la page de paiement en ligne.");
		if (booking.getStatus() == BookingStatus.DEMANDE) throw ApiException.badRequest("Envoyez d'abord le devis au client.");
		if (booking.getStatus() == BookingStatus.CLOTUREE || booking.getBalanceCents() <= 0)
			throw ApiException.badRequest("Cette réservation est déjà entièrement payée.");
		int amount = request.amountCents();
		if (amount > booking.getBalanceCents())
			throw ApiException.invalidField("amountCents", "Le montant dépasse le reste à payer (" + Money.euros(booking.getBalanceCents()) + ").");
		String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
		String label = Money.euros(amount) + " (" + request.method().label().toLowerCase() + ")";

		Payment payment;
		if (booking.getStatus() == BookingStatus.DEVIS) {
			if (amount < booking.getDepositCents())
				throw ApiException.invalidField("amountCents",
						"Il faut au moins l'acompte (" + Money.euros(booking.getDepositCents()) + ") pour confirmer la réservation.");
			String shortage = availability.shortage(booking, true);
			if (shortage != null) throw ApiException.conflict(shortage);
			payment = payments.save(Payment.offline(booking, request.method(), amount, note, author));
			booking.confirm("Acompte reçu : " + label, author);
		} else {
			payment = payments.save(Payment.offline(booking, request.method(), amount, note, author));
			booking.addNote("Paiement reçu : " + label, author);
		}
		invoices.forPayment(payment);
	}

	/** Paiement simule : la session doit appartenir au client connecte. */
	@Transactional(readOnly = true)
	public Payment simulatedSession(String sessionId, Long customerId) {
		if (!gateway.simulated()) throw ApiException.notFound("Paiement introuvable.");
		return payments.findByCheckoutSessionId(sessionId).filter(p -> p.getBooking().getCustomer().getId().equals(customerId))
				.orElseThrow(() -> ApiException.notFound("Paiement introuvable."));
	}

	@Transactional
	public Payment paySimulated(String sessionId, Long customerId) {
		Payment payment = simulatedSession(sessionId, customerId);
		completeCheckout(sessionId, "pi_simule_" + payment.getId());
		return payment;
	}
}
