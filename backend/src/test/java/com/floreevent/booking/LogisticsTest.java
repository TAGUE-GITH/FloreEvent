package com.floreevent.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.floreevent.common.Dates;
import com.floreevent.user.Role;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/** US-14 a US-17 : planning, bon de livraison, livraison avec caution, retour et casse (R7). */
class LogisticsTest extends BookingTestSupport {

	private static final LocalDate TOMORROW = Dates.today().plusDays(1);

	/** Reservation confirmee qui commence demain : la logistique peut deja la livrer. */
	private long tomorrowBooking(int tents) throws Exception {
		String lea = client();
		long id = idOf(postAs(lea, "/api/me/bookings", request(TOMORROW, TOMORROW.plusDays(1), "LIVRAISON", "5 rue Grande", false, this.tents, tents, chairs, 50)));
		postAs(manager, "/api/bookings/{id}/quote", "{}", id).andExpect(status().isOk());
		webhook(checkout(lea, id)).andExpect(status().isOk());
		return id;
	}

	@Test
	void thePlanningListsDeparturesAndReturnsWithTheEquipmentToLoad() throws Exception {
		long id = tomorrowBooking(2);
		quoted(client(), TOMORROW, TOMORROW, chairs, 10); // simple devis : pas dans le planning
		String driver = tokenFor(Role.LOGISTIQUE);

		getAs(driver, "/api/planning?from={from}&to={to}", Dates.today(), Dates.today().plusDays(6))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].bookingId").value(id))
				.andExpect(jsonPath("$[0].date").value(TOMORROW.toString()))
				.andExpect(jsonPath("$[0].kindLabel").value("Livraison"))
				.andExpect(jsonPath("$[0].done").value(false))
				.andExpect(jsonPath("$[0].address").value("5 Rue Grande 77300 Fontainebleau"))
				.andExpect(jsonPath("$[0].phone").value("06 12 34 56 78"))
				.andExpect(jsonPath("$[0].lines[0].itemName").value("Barnum 3 x 6 m"))
				.andExpect(jsonPath("$[0].lines[1].quantity").value(50))
				.andExpect(jsonPath("$[1].date").value(TOMORROW.plusDays(1).toString()))
				.andExpect(jsonPath("$[1].kindLabel").value("Reprise"));
		getAs(driver, "/api/planning?from={from}&to={to}", Dates.today(), Dates.today().plusDays(90))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Choisissez une période de deux mois au plus."));
		getAs(client(), "/api/planning?from={from}&to={to}", Dates.today(), Dates.today()).andExpect(status().isForbidden());
	}

	@Test
	void theDeliveryNoteListsTheEquipmentAndHasRoomForSignatures() throws Exception {
		long id = tomorrowBooking(2);
		byte[] pdf = getAs(manager, "/api/bookings/{id}/documents/bon-de-livraison", id).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();
		String text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1).replaceAll("[\\s ]+", " ");
		assertThat(text).contains("Bon de livraison", "Barnum 3 x 6 m", "Chaise Napoléon", "5 Rue Grande 77300 Fontainebleau",
				"Caution (non encaissée)", "BON POUR RÉCEPTION", "Nom, date et signature");
	}

	@Test
	void theDriverDeliversAndReceivesTheSecurityDeposit() throws Exception {
		long id = tomorrowBooking(2);
		String driver = tokenFor(Role.LOGISTIQUE);

		postAs(driver, "/api/bookings/{id}/deliver", "{\"securityDepositMethod\": \"CARTE\"}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.securityDepositMethod").value("La caution se remet par chèque, espèces ou virement."));
		postAs(driver, "/api/bookings/{id}/deliver", "{\"securityDepositMethod\": \"CHEQUE\"}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EN_COURS"))
				.andExpect(jsonPath("$.securityDepositMethod").value("CHEQUE"))
				.andExpect(jsonPath("$.events[3].author").value("Test LOGISTIQUE"));
		postAs(driver, "/api/bookings/{id}/deliver", "{\"securityDepositMethod\": \"CHEQUE\"}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Seule une réservation confirmée peut être livrée."));
		getAs(driver, "/api/planning?from={from}&to={to}", TOMORROW, TOMORROW).andExpect(jsonPath("$[0].done").value(true));

		// Une reservation dans un mois ne part pas aujourd'hui
		long later = paidByCard(client(), EVENT, EVENT, tents, 1);
		postAs(driver, "/api/bookings/{id}/deliver", "{\"securityDepositMethod\": \"ESPECES\"}", later)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Le matériel part au plus tôt la veille de l'événement."));
	}

	@Test
	void theReturnChargesBrokenAndMissingItemsOnTheSecurityDeposit() throws Exception {
		// 1 barnum sur 2 jours : 225 € ; 50 chaises : 262,50 € ; livraison 70 € ; caution = 30 % de 1 200 + 2 250 = 1 035 €
		long id = tomorrowBooking(1);
		String driver = tokenFor(Role.LOGISTIQUE);
		postAs(driver, "/api/bookings/{id}/deliver", "{\"securityDepositMethod\": \"CHEQUE\"}", id);

		postAs(driver, "/api/bookings/{id}/return", """
				{"lines": [{"itemId": %d, "returned": 1, "damaged": 0, "missing": 0},
				           {"itemId": %d, "returned": 45, "damaged": 2, "missing": 2}]}""".formatted(tents, chairs), id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Chaise Napoléon : 50 loué(s), le total rendu + abîmé + manquant doit faire 50."));

		// 2 chaises abimees + 1 manquante : 3 x 45 € = 135 €, retenus sur la caution
		postAs(driver, "/api/bookings/{id}/return", """
				{"lines": [{"itemId": %d, "returned": 1, "damaged": 0, "missing": 0},
				           {"itemId": %d, "returned": 47, "damaged": 2, "missing": 1}], "note": "Pied cassé"}""".formatted(tents, chairs), id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RETOURNEE"))
				.andExpect(jsonPath("$.damageCents").value(13500))
				.andExpect(jsonPath("$.securityDepositReturnedCents").value(103500 - 13500))
				.andExpect(jsonPath("$.lines[1].damagedQuantity").value(2))
				.andExpect(jsonPath("$.lines[1].missingQuantity").value(1))
				.andExpect(jsonPath("$.payments[1].methodLabel").value("Chèque"))
				.andExpect(jsonPath("$.payments[1].amountCents").value(13500))
				.andExpect(jsonPath("$.invoices[1].kind").value("ACOMPTE"))
				.andExpect(jsonPath("$.events[4].note")
						.value("Casse et manquants : 135,00 €, dont 135,00 € retenus sur la caution. Pied cassé"));

		// Les chaises abimees partent en reparation, la manquante sort du stock
		getAs(manager, "/api/items/{id}", chairs)
				.andExpect(jsonPath("$.quantity").value(119))
				.andExpect(jsonPath("$.inRepair").value(2));

		// Le solde doit etre paye avant de cloturer
		postAs(manager, "/api/bookings/{id}/close", "{}", id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Il reste 390,25 € à payer avant de clôturer."));
		postAs(driver, "/api/bookings/{id}/payments", "{\"method\": \"VIREMENT\", \"amountCents\": 39025}", id)
				.andExpect(jsonPath("$.balanceCents").value(0))
				.andExpect(jsonPath("$.invoices[2].kind").value("SOLDE"));
		postAs(driver, "/api/bookings/{id}/close", "{}", id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOTUREE"))
				.andExpect(jsonPath("$.events[6].note").value("Caution restituée : 900,00 €"));
	}

	@Test
	void everythingReturnedInGoodConditionCostsNothing() throws Exception {
		long id = tomorrowBooking(2);
		postAs(manager, "/api/bookings/{id}/return", """
				{"lines": [{"itemId": %d, "returned": 2, "damaged": 0, "missing": 0}, {"itemId": %d, "returned": 50, "damaged": 0, "missing": 0}]}"""
				.formatted(tents, chairs), id)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Le matériel n'a pas encore été livré."));
		postAs(manager, "/api/bookings/{id}/deliver", "{\"securityDepositMethod\": \"ESPECES\"}", id);
		postAs(manager, "/api/bookings/{id}/return", """
				{"lines": [{"itemId": %d, "returned": 2, "damaged": 0, "missing": 0}, {"itemId": %d, "returned": 50, "damaged": 0, "missing": 0}]}"""
				.formatted(tents, chairs), id)
				.andExpect(jsonPath("$.damageCents").value(0))
				.andExpect(jsonPath("$.events[4].note").value("Matériel rendu complet et en bon état"))
				.andExpect(jsonPath("$.payments.length()").value(1));
	}
}
