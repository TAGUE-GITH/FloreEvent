package com.floreevent.common;

import java.time.LocalDate;
import java.time.ZoneId;

/** FloreEvent travaille a l'heure de Paris : « aujourd'hui » est toujours la date a Melun. */
public final class Dates {

	public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

	private Dates() {
	}

	public static LocalDate today() {
		return LocalDate.now(ZONE);
	}
}
