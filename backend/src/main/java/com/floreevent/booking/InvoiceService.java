package com.floreevent.booking;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.common.Dates;

/** R6 : factures numerotees FAC-2026-00001, a la suite et sans trou. */
@Service
public class InvoiceService {

	private final JdbcTemplate jdbc;
	private final InvoiceRepository invoices;

	public InvoiceService(JdbcTemplate jdbc, InvoiceRepository invoices) {
		this.jdbc = jdbc;
		this.invoices = invoices;
	}

	/**
	 * Le compteur de l'annee reste verrouille jusqu'a la fin de la transaction (UPDATE) : si l'operation echoue, le numero
	 * n'est pas consomme ; deux factures simultanees attendent leur tour.
	 */
	@Transactional
	public Invoice issue(Booking booking, Payment payment, InvoiceKind kind, int amountCents) {
		int year = Dates.today().getYear();
		if (jdbc.update("update invoice_counter set last_number = last_number + 1 where invoice_year = ?", year) == 0)
			jdbc.update("insert into invoice_counter (invoice_year, last_number) values (?, 1)", year);
		Integer number = jdbc.queryForObject("select last_number from invoice_counter where invoice_year = ?", Integer.class, year);
		return invoices.save(new Invoice("FAC-%d-%05d".formatted(year, number), booking, payment, kind, amountCents));
	}

	/** Facture d'un paiement encaisse : acompte tant qu'il reste a payer, solde sinon. */
	@Transactional
	public Invoice forPayment(Payment payment) {
		Booking booking = payment.getBooking();
		InvoiceKind kind = booking.getBalanceCents() > 0 ? InvoiceKind.ACOMPTE : InvoiceKind.SOLDE;
		return issue(booking, payment, kind, payment.getNetCents());
	}
}
