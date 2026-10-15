package com.floreevent.booking;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.common.ApiException;
import com.floreevent.common.Dates;
import com.floreevent.common.Money;
import com.floreevent.payment.PaymentGateway;
import com.floreevent.user.User;

/**
 * US-13 : annulation selon la date (cahier des charges) : plus de 30 jours avant l'evenement, l'acompte est rembourse ;
 * de 30 a 8 jours, l'acompte est conserve ; moins de 8 jours, la moitie du total reste due.
 */
@Service
public class CancellationService {

	/** Ce que l'annulation coute : somme gardee, a rembourser, encore due ; regle appliquee. */
	public record Preview(int retainedCents, int refundCents, int dueCents, String rule) {
	}

	private final PaymentGateway gateway;
	private final InvoiceService invoices;
	private final int fullRefundDays;
	private final int depositKeptDays;
	private final int latePercent;

	public CancellationService(PaymentGateway gateway, InvoiceService invoices,
			@Value("${app.cancellation.full-refund-days}") int fullRefundDays,
			@Value("${app.cancellation.deposit-kept-days}") int depositKeptDays,
			@Value("${app.cancellation.late-percent}") int latePercent) {
		this.gateway = gateway;
		this.invoices = invoices;
		this.fullRefundDays = fullRefundDays;
		this.depositKeptDays = depositKeptDays;
		this.latePercent = latePercent;
	}

	public Preview preview(Booking booking) {
		BookingStatus status = booking.getStatus();
		if (status == BookingStatus.DEMANDE || status == BookingStatus.DEVIS)
			return new Preview(0, 0, 0, "Sans frais : la réservation n'est pas encore confirmée.");
		if (status != BookingStatus.CONFIRMEE)
			throw ApiException.badRequest("Le matériel est déjà livré : cette réservation ne peut plus être annulée.");
		long daysBefore = booking.getStartDate().toEpochDay() - Dates.today().toEpochDay();
		int retained;
		String rule;
		if (daysBefore > fullRefundDays) {
			retained = 0;
			rule = "Plus de " + fullRefundDays + " jours avant l'événement : l'acompte est remboursé.";
		} else if (daysBefore >= depositKeptDays) {
			retained = Math.min(booking.getDepositCents(), booking.getTotalCents());
			rule = "Entre " + fullRefundDays + " et " + depositKeptDays + " jours avant l'événement : l'acompte est conservé.";
		} else {
			retained = (int) Math.round(booking.getTotalCents() * latePercent / 100.0);
			rule = "Moins de " + depositKeptDays + " jours avant l'événement : " + latePercent + " % du total reste dû.";
		}
		int paid = booking.getPaidCents();
		return new Preview(retained, Math.max(0, paid - retained), Math.max(0, retained - paid), rule);
	}

	/** Annulation par le client ou par l'equipe ; les sommes en trop sont remboursees, un avoir est emis (R6). */
	@Transactional
	public void cancel(Booking booking, String reason, User author, boolean byCustomer) {
		BookingStatus status = booking.getStatus();
		if (status == BookingStatus.DEMANDE || status == BookingStatus.DEVIS) {
			if (byCustomer) booking.cancelByCustomer(author);
			else booking.refuse(reason, author);
			return;
		}
		Preview preview = preview(booking);
		String who = byCustomer ? "Annulée par le client" : "Annulée par FloreEvent : " + reason;
		booking.cancelConfirmed(preview.retainedCents(), who + ". " + preview.rule(), author);
		if (preview.refundCents() > 0) refund(booking, preview.refundCents(), author);
	}

	/** Les paiements les plus recents sont rembourses d'abord ; la carte est remboursee par Stripe, le reste par l'equipe. */
	private void refund(Booking booking, int amount, User author) {
		int remaining = amount;
		boolean offline = false;
		for (Payment payment : List.copyOf(booking.getPayments()).reversed()) {
			if (remaining == 0) break;
			int part = Math.min(remaining, payment.getNetCents());
			if (part <= 0) continue;
			if (payment.getMethod() == PaymentMethod.CARTE) gateway.refund(payment.getPaymentIntentId(), part);
			else offline = true;
			payment.refund(part);
			remaining -= part;
		}
		invoices.issue(booking, null, InvoiceKind.AVOIR, -amount);
		booking.addNote("Remboursement de " + Money.euros(amount) + (offline ? " (à rendre au client pour les paiements hors ligne)" : ""),
				author);
	}
}
