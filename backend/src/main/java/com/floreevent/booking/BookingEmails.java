package com.floreevent.booking;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.floreevent.common.Dates;
import com.floreevent.common.Money;
import com.floreevent.mail.EmailService;

/** US-19 : emails au client a chaque etape importante (devis, confirmation, rappel). */
@Component
public class BookingEmails {

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

	private final EmailService emails;
	private final String frontendUrl;

	public BookingEmails(EmailService emails, @Value("${app.frontend-url}") String frontendUrl) {
		this.emails = emails;
		this.frontendUrl = frontendUrl;
	}

	private String link(Booking b) {
		return frontendUrl + "/mon-espace/reservations/" + b.getId();
	}

	private static String greeting(Booking b) {
		return "Bonjour " + b.getCustomer().getFullName() + ",";
	}

	private static String period(Booking b) {
		return b.getStartDate().equals(b.getEndDate()) ? "le " + b.getStartDate().format(DAY)
				: "du " + b.getStartDate().format(DAY) + " au " + b.getEndDate().format(DAY);
	}

	void quoteSent(Booking b) {
		emails.send(b.getCustomer().getEmail(), "Votre devis FloreEvent " + b.getReference(), greeting(b),
				List.of("Votre devis pour la location " + period(b) + " est prêt : " + Money.euros(b.getTotalCents()) + " TTC.",
						"Pour confirmer votre réservation, acceptez-le et payez l'acompte de " + Money.euros(b.getDepositCents())
								+ " en ligne avant le " + b.getQuoteValidUntil().format(DAY) + "."),
				"Voir mon devis", link(b), "Le matériel n'est réservé qu'au paiement de l'acompte.");
	}

	void confirmed(Booking b) {
		emails.send(b.getCustomer().getEmail(), "Réservation confirmée " + b.getReference(), greeting(b),
				List.of("Merci ! Votre réservation " + period(b) + " est confirmée.",
						"Reste à payer : " + Money.euros(Math.max(0, b.getBalanceCents())) + ", au plus tard à la livraison. "
								+ "Une caution de " + Money.euros(b.getSecurityDepositCents()) + " (non encaissée) vous sera demandée."),
				"Voir ma réservation et ma facture", link(b), null);
	}

	void reminder(Booking b) {
		String when = b.getDeliveryMode() == DeliveryMode.LIVRAISON ? "Nous livrons votre matériel le " + b.getStartDate().format(DAY)
				+ " à l'adresse : " + b.getAddress() + "."
				: "Votre matériel vous attend à notre entrepôt de Melun le " + b.getStartDate().format(DAY) + ".";
		emails.send(b.getCustomer().getEmail(), "Votre événement approche · " + b.getReference(), greeting(b),
				List.of(when, "Pensez à préparer la caution de " + Money.euros(b.getSecurityDepositCents())
						+ (b.getBalanceCents() > 0 ? " et le solde de " + Money.euros(b.getBalanceCents()) : "") + "."),
				"Voir ma réservation", link(b), "Envoyé le " + Dates.today().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
	}
}
