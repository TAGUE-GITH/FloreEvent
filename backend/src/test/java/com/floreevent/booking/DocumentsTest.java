package com.floreevent.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.floreevent.common.Dates;
import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/** US-11 : devis, confirmation et factures en PDF, pour le client et pour l'equipe. */
class DocumentsTest extends BookingTestSupport {

	private static String text(byte[] pdf) throws Exception {
		return new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1).replaceAll("[\\s\u00a0]+", " ");
	}

	@Test
	void theQuoteDetailsPricesDiscountAndValidity() throws Exception {
		String lea = client();
		long id = idOf(postAs(lea, "/api/me/bookings", request(EVENT, EVENT.plusDays(2), "LIVRAISON", "5 rue Grande", true, tents, 2, chairs, 100)));
		getAs(lea, "/api/me/bookings/{id}/documents/devis", id).andExpect(status().isNotFound());
		postAs(manager, "/api/bookings/{id}/quote", "{\"discountCents\": 3000, \"message\": \"Merci pour votre confiance\"}", id);

		byte[] pdf = getAs(lea, "/api/me/bookings/{id}/documents/devis", id)
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/pdf"))
				.andReturn().getResponse().getContentAsByteArray();
		String reference = JsonPath.read(body(getAs(lea, "/api/me/bookings/{id}", id)), "$.reference");

		// 1 530 € - 30 € de remise = 1 500 € ; acompte 450 €
		assertThat(text(pdf)).contains("Devis", reference, "Léa Moreau", "Barnum 3 x 6 m", "Chaise Napoléon", "Livraison et reprise (19 km)",
				"Remise", "-30,00 €", "Total TTC", "1 500,00 €", "TVA 20 %", "250,00 €", "450,00 €", "Merci pour votre confiance",
				"Devis valable jusqu'au", "SIRET : à compléter");
	}

	@Test
	void confirmationAndInvoiceAfterPayment() throws Exception {
		String lea = client();
		long id = paidByCard(lea, EVENT, EVENT.plusDays(2), tents, 2);
		getAs(manager, "/api/bookings/{id}/documents/inconnu", id).andExpect(status().isNotFound());

		String confirmation = text(getAs(lea, "/api/me/bookings/{id}/documents/confirmation", id).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray());
		assertThat(confirmation).contains("Confirmation de réservation", "Carte bancaire", "180,00 €", "Reste à payer : 420,00 €");

		Number invoiceId = JsonPath.read(body(getAs(lea, "/api/me/bookings/{id}", id)), "$.invoices[0].id");
		String invoice = text(getAs(manager, "/api/bookings/{id}/invoices/{invoice}", id, invoiceId).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray());
		assertThat(invoice).contains("Facture d'acompte", "FAC-" + Dates.today().getYear() + "-00001", "Total HT", "150,00 €", "30,00 €",
				"180,00 €", "carte bancaire");

		// Les documents d'un client ne sont visibles que par lui
		getAs(client(), "/api/me/bookings/{id}/invoices/{invoice}", id, invoiceId).andExpect(status().isNotFound());
		getAs(client(), "/api/me/bookings/{id}/documents/confirmation", id).andExpect(status().isNotFound());
	}
}
