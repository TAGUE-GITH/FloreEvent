package com.floreevent.booking;

import java.util.List;

/** Cycle de vie d'une reservation (R4). Seules CONFIRMEE et EN_COURS bloquent le materiel (R1). */
public enum BookingStatus {
	DEMANDE("Demande envoyée"),
	DEVIS("Devis envoyé"),
	CONFIRMEE("Confirmée"),
	EN_COURS("En cours"),
	RETOURNEE("Matériel rendu"),
	CLOTUREE("Clôturée"),
	ANNULEE("Annulée");

	private final String label;

	BookingStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** Reservations qui immobilisent le materiel. */
	public static final List<BookingStatus> BLOCKING = List.of(CONFIRMEE, EN_COURS);
}
