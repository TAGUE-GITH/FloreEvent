package com.floreevent.booking;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.floreevent.common.Dates;

/**
 * US-13 : plus de 30 jours avant l'evenement, l'acompte est rembourse ; de 30 a 8 jours, il est conserve ; moins de
 * 8 jours, 50 % du total reste du.
 */
class CancellationTest extends BookingTestSupport {

	@Test
	void moreThan30DaysBeforeTheDepositIsRefunded() throws Exception {
		String lea = client();
		// 2 barnums sur 1 jour : 300 €, acompte 90 €
		long id = paidByCard(lea, Dates.today().plusDays(40), Dates.today().plusDays(40), tents, 2);

		getAs(lea, "/api/me/bookings/{id}/cancellation", id)
				.andExpect(jsonPath("$.retainedCents").value(0))
				.andExpect(jsonPath("$.refundCents").value(9000))
				.andExpect(jsonPath("$.dueCents").value(0))
				.andExpect(jsonPath("$.rule").value("Plus de 30 jours avant l'événement : l'acompte est remboursé."));

		postAs(lea, "/api/me/bookings/{id}/cancel", "{}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ANNULEE"))
				.andExpect(jsonPath("$.cancellationFeeCents").value(0))
				.andExpect(jsonPath("$.paidCents").value(0))
				.andExpect(jsonPath("$.balanceCents").value(0))
				.andExpect(jsonPath("$.payments[0].refundedCents").value(9000))
				.andExpect(jsonPath("$.invoices[1].kindLabel").value("Avoir"))
				.andExpect(jsonPath("$.invoices[1].amountCents").value(-9000))
				.andExpect(jsonPath("$.events[3].note")
						.value("Annulée par le client. Plus de 30 jours avant l'événement : l'acompte est remboursé."))
				.andExpect(jsonPath("$.events[4].note").value("Remboursement de 90,00\u00a0€"));
		verify(gateway).refund(anyString(), org.mockito.ArgumentMatchers.eq(9000));
		// Le materiel est de nouveau libre
		postAs(null, "/api/quote/estimate", request(Dates.today().plusDays(40), Dates.today().plusDays(40), "RETRAIT", null, false, tents, 4))
				.andExpect(jsonPath("$.bookable").value(true));
	}

	@Test
	void between30And8DaysTheDepositIsKept() throws Exception {
		String lea = client();
		long id = paidByCard(lea, Dates.today().plusDays(20), Dates.today().plusDays(20), tents, 2);

		postAs(lea, "/api/me/bookings/{id}/cancel", "{}", id)
				.andExpect(jsonPath("$.status").value("ANNULEE"))
				.andExpect(jsonPath("$.cancellationFeeCents").value(9000))
				.andExpect(jsonPath("$.amountDueCents").value(9000))
				.andExpect(jsonPath("$.paidCents").value(9000))
				.andExpect(jsonPath("$.balanceCents").value(0))
				.andExpect(jsonPath("$.invoices.length()").value(1));
		verify(gateway, never()).refund(anyString(), anyInt());
	}

	@Test
	void lessThan8DaysHalfTheTotalIsDue() throws Exception {
		String lea = client();
		long id = quoted(lea, Dates.today().plusDays(5), Dates.today().plusDays(5), tents, 2);
		postAs(manager, "/api/bookings/{id}/payments", "{\"method\": \"VIREMENT\", \"amountCents\": 9000}", id).andExpect(status().isOk());

		getAs(manager, "/api/bookings/{id}/cancellation", id)
				.andExpect(jsonPath("$.retainedCents").value(15000))
				.andExpect(jsonPath("$.dueCents").value(6000))
				.andExpect(jsonPath("$.rule").value("Moins de 8 jours avant l'événement : 50 % du total reste dû."));
		postAs(manager, "/api/bookings/{id}/cancel", "{\"reason\": \"Événement reporté par le client\"}", id)
				.andExpect(jsonPath("$.status").value("ANNULEE"))
				.andExpect(jsonPath("$.balanceCents").value(6000));
		// Le reste du est encaisse ensuite
		postAs(manager, "/api/bookings/{id}/payments", "{\"method\": \"CHEQUE\", \"amountCents\": 6000}", id)
				.andExpect(jsonPath("$.balanceCents").value(0))
				.andExpect(jsonPath("$.invoices[1].kind").value("SOLDE"));
	}

	@Test
	void deliveredEquipmentCanNoLongerBeCancelled() throws Exception {
		String lea = client();
		long id = paidByCard(lea, EVENT, EVENT, tents, 1);
		entityManager.flush();
		jdbc.update("update booking set status = 'EN_COURS' where id = ?", id);
		entityManager.clear();

		postAs(lea, "/api/me/bookings/{id}/cancel", "{}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Le matériel est déjà livré : cette réservation ne peut plus être annulée."));
	}
}
