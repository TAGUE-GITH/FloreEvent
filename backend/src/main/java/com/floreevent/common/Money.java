package com.floreevent.common;

/** Montants en centimes affiches a la francaise : « 1 530,00 € » (espaces insecables, lisibles aussi dans les PDF). */
public final class Money {

	private static final char SPACE = ' ';

	private Money() {
	}

	public static String euros(int cents) {
		String sign = cents < 0 ? "-" : "";
		long abs = Math.abs((long) cents);
		String units = String.valueOf(abs / 100);
		var grouped = new StringBuilder();
		for (int i = 0; i < units.length(); i++) {
			if (i > 0 && (units.length() - i) % 3 == 0) grouped.append(SPACE);
			grouped.append(units.charAt(i));
		}
		return sign + grouped + "," + "%02d".formatted(abs % 100) + SPACE + "€";
	}
}
