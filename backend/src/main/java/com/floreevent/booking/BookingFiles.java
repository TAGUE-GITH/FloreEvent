package com.floreevent.booking;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.floreevent.common.ApiException;
import com.floreevent.document.BookingDocuments;

/** US-11 : documents PDF d'une reservation, communs a l'equipe et au client. */
@Component
class BookingFiles {

	private final BookingDocuments documents;

	BookingFiles(BookingDocuments documents) {
		this.documents = documents;
	}

	/** « devis » des que le devis est envoye, « confirmation » une fois l'acompte paye. */
	ResponseEntity<byte[]> document(Booking booking, String kind) {
		return switch (kind) {
			case "devis" -> {
				if (booking.getQuoteValidUntil() == null) throw ApiException.notFound("Le devis n'est pas encore prêt.");
				yield pdf(documents.quote(booking), "Devis-" + booking.getReference() + ".pdf");
			}
			case "confirmation" -> {
				if (!wasConfirmed(booking.getStatus())) throw ApiException.notFound("La réservation n'est pas encore confirmée.");
				yield pdf(documents.confirmation(booking), "Confirmation-" + booking.getReference() + ".pdf");
			}
			default -> throw ApiException.notFound("Document introuvable.");
		};
	}

	ResponseEntity<byte[]> invoice(Booking booking, Long invoiceId) {
		Invoice invoice = booking.getInvoices().stream().filter(i -> i.getId().equals(invoiceId)).findFirst()
				.orElseThrow(() -> ApiException.notFound("Facture introuvable."));
		return pdf(documents.invoice(invoice), invoice.getNumber() + ".pdf");
	}

	private static boolean wasConfirmed(BookingStatus status) {
		return status == BookingStatus.CONFIRMEE || status == BookingStatus.EN_COURS || status == BookingStatus.RETOURNEE
				|| status == BookingStatus.CLOTUREE;
	}

	private static ResponseEntity<byte[]> pdf(byte[] content, String filename) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString()).body(content);
	}
}
