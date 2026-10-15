package com.floreevent.document;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.floreevent.booking.Booking;
import com.floreevent.booking.BookingLine;
import com.floreevent.booking.DeliveryMode;
import com.floreevent.booking.Invoice;
import com.floreevent.booking.InvoiceKind;
import com.floreevent.booking.Payment;
import com.floreevent.common.Dates;
import com.floreevent.common.Money;
import com.floreevent.quote.Pricing;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/** US-11 : devis, confirmation de reservation et factures au format A4. */
@Component
public class BookingDocuments {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final Color BRAND = new Color(4, 120, 87);
	private static final Color MUTED = new Color(87, 83, 78);
	private static final Color LINE = new Color(214, 211, 209);
	private static final Font COMPANY = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND);
	private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
	private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f);
	private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 9.5f);
	private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
	private static final Font HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

	private final CompanyInfo company;

	public BookingDocuments(CompanyInfo company) {
		this.company = company;
	}

	/** Devis : detail des prix, acompte, validite et conditions d'annulation. */
	public byte[] quote(Booking b) {
		return render(document -> {
			header(document, "Devis", "Réservation " + b.getReference(), "Date : " + date(Dates.today()));
			customerAndEvent(document, b);
			lines(document, b);
			totals(document, b);
			if (b.getQuoteMessage() != null) paragraph(document, "Message de FloreEvent : " + b.getQuoteMessage(), TEXT, 10);
			paragraph(document, "Acompte à la réservation : " + Money.euros(b.getDepositCents())
					+ ". La réservation est confirmée au paiement de l'acompte ; le solde est dû au plus tard à la livraison.", TEXT, 10);
			if (b.getQuoteValidUntil() != null)
				paragraph(document, "Devis valable jusqu'au " + date(b.getQuoteValidUntil()) + ".", BOLD, 4);
			conditions(document, b);
		});
	}

	/** Confirmation : reservation confirmee, paiements recus et reste a payer. */
	public byte[] confirmation(Booking b) {
		return render(document -> {
			header(document, "Confirmation de réservation", "Réservation " + b.getReference(), "Date : " + date(Dates.today()));
			customerAndEvent(document, b);
			lines(document, b);
			totals(document, b);
			var paid = table(new float[] { 4, 3, 2 });
			headCells(paid, "Paiement reçu", "Date", "Montant");
			for (Payment p : b.getPayments()) {
				if (p.getNetCents() <= 0) continue;
				cell(paid, p.getMethod().label(), TEXT, Element.ALIGN_LEFT);
				cell(paid, date(p.getPaidAt()), TEXT, Element.ALIGN_LEFT);
				cell(paid, Money.euros(p.getNetCents()), TEXT, Element.ALIGN_RIGHT);
			}
			paid.setSpacingBefore(14);
			document.add(paid);
			paragraph(document, "Reste à payer : " + Money.euros(Math.max(0, b.getBalanceCents())) + " (au plus tard à la livraison).", BOLD, 8);
			conditions(document, b);
		});
	}

	/** US-15 : bon de livraison a faire signer par le client, avec la liste du materiel a controler. */
	public byte[] deliveryNote(Booking b) {
		return render(document -> {
			header(document, "Bon de livraison", "Réservation " + b.getReference(), "Date : " + date(b.getStartDate()));
			customerAndEvent(document, b);
			var table = table(new float[] { 6, 1.2f, 2, 2 });
			table.setSpacingBefore(14);
			headCells(table, "Article", "Qté", "Livré", "Rendu");
			for (BookingLine line : b.getLines()) {
				cell(table, line.getItemName(), TEXT, Element.ALIGN_LEFT);
				cell(table, String.valueOf(line.getQuantity()), TEXT, Element.ALIGN_RIGHT);
				cell(table, " ", TEXT, Element.ALIGN_RIGHT);
				cell(table, " ", TEXT, Element.ALIGN_RIGHT);
			}
			document.add(table);
			paragraph(document, "Caution (non encaissée) : " + Money.euros(b.getSecurityDepositCents())
					+ ". Le matériel abîmé ou manquant au retour est facturé à sa valeur de remplacement et retenu sur la caution.", TEXT, 10);
			paragraph(document, "Reste à payer à la livraison : " + Money.euros(Math.max(0, b.getBalanceCents())) + ".", BOLD, 4);
			var signatures = table(new float[] { 1, 1 });
			signatures.setSpacingBefore(24);
			for (String who : new String[] { "Pour FloreEvent", "Le client : « bon pour réception »" }) {
				var cell = box(who);
				cell.setFixedHeight(90);
				cell.addElement(new Paragraph("Nom, date et signature", SMALL));
				signatures.addCell(cell);
			}
			document.add(signatures);
		});
	}

	/** Facture d'acompte, de solde, ou avoir ; montants TTC avec HT et TVA. */
	public byte[] invoice(Invoice invoice) {
		Booking b = invoice.getBooking();
		return render(document -> {
			header(document, invoice.getKind().label(), "N° " + invoice.getNumber(), "Date : " + date(invoice.getIssuedAt()));
			customerAndEvent(document, b);
			if (invoice.getKind() == InvoiceKind.SOLDE) lines(document, b);

			var table = table(new float[] { 6, 2 });
			table.setSpacingBefore(14);
			headCells(table, "Désignation", "Montant TTC");
			String label = switch (invoice.getKind()) {
				case ACOMPTE -> "Acompte sur la réservation " + b.getReference() + " (total TTC " + Money.euros(b.getTotalCents()) + ")";
				case SOLDE -> "Règlement de la réservation " + b.getReference() + " (total TTC " + Money.euros(b.getAmountDueCents()) + ")";
				case AVOIR -> "Remboursement sur la réservation " + b.getReference();
			};
			cell(table, label, TEXT, Element.ALIGN_LEFT);
			cell(table, Money.euros(invoice.getAmountCents()), TEXT, Element.ALIGN_RIGHT);
			int vat = Pricing.vatIncluded(invoice.getAmountCents());
			total(table, "Total HT", Money.euros(invoice.getAmountCents() - vat), TEXT);
			total(table, "TVA 20 %", Money.euros(vat), TEXT);
			total(table, "Total TTC", Money.euros(invoice.getAmountCents()), BOLD);
			document.add(table);

			Payment p = invoice.getPayment();
			if (p != null) paragraph(document, "Payé le " + date(p.getPaidAt()) + " par " + p.getMethod().label().toLowerCase() + ".", TEXT, 10);
			if (invoice.getKind() == InvoiceKind.AVOIR) paragraph(document, "Avoir émis suite à l'annulation de la réservation.", TEXT, 10);
		});
	}

	private interface Content {
		void write(Document document);
	}

	private static byte[] render(Content content) {
		var out = new ByteArrayOutputStream();
		var document = new Document(PageSize.A4, 42, 42, 40, 40);
		PdfWriter.getInstance(document, out);
		document.open();
		content.write(document);
		document.close();
		return out.toByteArray();
	}

	private void header(Document document, String title, String number, String when) {
		var table = table(new float[] { 1, 1 });
		var left = new PdfPCell();
		left.setBorder(Rectangle.NO_BORDER);
		left.addElement(new Paragraph(company.name(), COMPANY));
		left.addElement(new Paragraph("Location de matériel pour événements", SMALL));
		left.addElement(new Paragraph(company.address(), SMALL));
		if (!company.phone().isBlank()) left.addElement(new Paragraph(company.phone(), SMALL));
		left.addElement(new Paragraph(company.email(), SMALL));
		left.addElement(new Paragraph(company.siretLine(), SMALL));
		table.addCell(left);
		var right = new PdfPCell();
		right.setBorder(Rectangle.NO_BORDER);
		for (var line : new Paragraph[] { new Paragraph(title, TITLE), new Paragraph(number, BOLD), new Paragraph(when, TEXT) }) {
			line.setAlignment(Element.ALIGN_RIGHT);
			right.addElement(line);
		}
		table.addCell(right);
		document.add(table);
	}

	private static void customerAndEvent(Document document, Booking b) {
		var c = b.getCustomer();
		var table = table(new float[] { 1, 1 });
		table.setSpacingBefore(18);
		var client = box("Client");
		client.addElement(new Paragraph(c.getFullName(), BOLD));
		if (c.getCompany() != null) client.addElement(new Paragraph(c.getCompany(), TEXT));
		client.addElement(new Paragraph(c.getPhone(), TEXT));
		if (c.getEmail() != null) client.addElement(new Paragraph(c.getEmail(), TEXT));
		table.addCell(client);
		var event = box("Événement");
		event.addElement(new Paragraph("Du " + date(b.getStartDate()) + " au " + date(b.getEndDate()) + " (" + b.getDays() + " jour"
				+ (b.getDays() > 1 ? "s" : "") + ")", TEXT));
		if (b.getDeliveryMode() == DeliveryMode.LIVRAISON) {
			event.addElement(new Paragraph("Livraison et reprise" + (b.isInstallation() ? ", avec installation" : ""), TEXT));
			event.addElement(new Paragraph(b.getAddress(), TEXT));
		} else event.addElement(new Paragraph("Retrait et retour à notre entrepôt de Melun", TEXT));
		table.addCell(event);
		document.add(table);
	}

	private static PdfPCell box(String title) {
		var cell = new PdfPCell();
		cell.setBorderColor(LINE);
		cell.setPadding(8);
		cell.addElement(new Paragraph(title.toUpperCase(), SMALL));
		return cell;
	}

	private static void lines(Document document, Booking b) {
		var table = table(new float[] { 6, 1.2f, 2, 2 });
		table.setSpacingBefore(14);
		headCells(table, "Article", "Qté", "Prix / jour", "Total");
		for (BookingLine line : b.getLines()) {
			cell(table, line.getItemName(), TEXT, Element.ALIGN_LEFT);
			cell(table, String.valueOf(line.getQuantity()), TEXT, Element.ALIGN_RIGHT);
			cell(table, Money.euros(line.getDayPriceCents()), TEXT, Element.ALIGN_RIGHT);
			cell(table, Money.euros(line.getLineTotalCents()), TEXT, Element.ALIGN_RIGHT);
		}
		document.add(table);
		if (b.getDays() > 1) paragraph(document, "Premier jour au tarif plein, jours suivants à 50 %.", SMALL, 3);
	}

	private static void totals(Document document, Booking b) {
		var table = table(new float[] { 6, 2 });
		table.setSpacingBefore(8);
		total(table, "Location", Money.euros(b.getRentalCents()), TEXT);
		if (b.getSetupCents() > 0) total(table, "Installation et démontage", Money.euros(b.getSetupCents()), TEXT);
		if (b.getDeliveryMode() == DeliveryMode.LIVRAISON)
			total(table, "Livraison et reprise (" + b.getDistanceKm() + " km)", Money.euros(b.getDeliveryCents()), TEXT);
		if (b.getDiscountCents() > 0) total(table, "Remise", Money.euros(-b.getDiscountCents()), TEXT);
		int vat = Pricing.vatIncluded(b.getTotalCents());
		total(table, "Total HT", Money.euros(b.getTotalCents() - vat), TEXT);
		total(table, "TVA 20 %", Money.euros(vat), TEXT);
		total(table, "Total TTC", Money.euros(b.getTotalCents()), BOLD);
		if (b.getDamageCents() > 0) total(table, "Casse et manquants (TTC)", Money.euros(b.getDamageCents()), BOLD);
		document.add(table);
		paragraph(document, "Caution demandée à la livraison ou au retrait (non encaissée) : " + Money.euros(b.getSecurityDepositCents()) + ".",
				SMALL, 4);
	}

	private static void conditions(Document document, Booking b) {
		paragraph(document, "Annulation : plus de 30 jours avant l'événement, l'acompte est remboursé ; de 30 à 8 jours, l'acompte est conservé ;"
				+ " moins de 8 jours, 50 % du total reste dû. Conditions générales de location acceptées (version "
				+ (b.getTermsVersion() == null ? "en vigueur" : b.getTermsVersion()) + ").", SMALL, 16);
	}

	private static PdfPTable table(float[] widths) {
		var table = new PdfPTable(widths);
		table.setWidthPercentage(100);
		return table;
	}

	private static void headCells(PdfPTable table, String... titles) {
		for (int i = 0; i < titles.length; i++) {
			var cell = new PdfPCell(new Phrase(titles[i], HEAD));
			cell.setBackgroundColor(BRAND);
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setPadding(5);
			cell.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
			table.addCell(cell);
		}
	}

	private static void cell(PdfPTable table, String text, Font font, int align) {
		var cell = new PdfPCell(new Phrase(text, font));
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(LINE);
		cell.setPadding(5);
		cell.setHorizontalAlignment(align);
		table.addCell(cell);
	}

	private static void total(PdfPTable table, String label, String amount, Font font) {
		for (var text : new String[] { label, amount }) {
			var cell = new PdfPCell(new Phrase(text, font));
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setPadding(3);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			table.addCell(cell);
		}
	}

	private static void paragraph(Document document, String text, Font font, float spacingBefore) {
		var paragraph = new Paragraph(text, font);
		paragraph.setSpacingBefore(spacingBefore);
		document.add(paragraph);
	}

	private static String date(LocalDate day) {
		return day.format(DATE);
	}

	private static String date(Instant instant) {
		return instant == null ? "" : instant.atZone(Dates.ZONE).format(DATE);
	}
}
