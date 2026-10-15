package com.floreevent.booking;

import java.time.LocalDate;
import java.util.List;

import com.floreevent.catalog.Item;

/** Prix calcule pour une selection d'articles et des dates, avec la disponibilite de chaque article. */
public record Estimate(LocalDate startDate, LocalDate endDate, int days, DeliveryMode deliveryMode, String address,
		Integer distanceKm, boolean installation, List<Line> lines, int rentalCents, int setupCents, int deliveryCents,
		int totalCents, int vatCents, int depositCents, int securityDepositCents, List<String> problems) {

	/** available : exemplaires libres aux dates demandees. */
	public record Line(Item item, int quantity, int available, int lineTotalCents) {
	}

	/** Tout est disponible : la demande peut etre envoyee. */
	public boolean bookable() {
		return problems.isEmpty();
	}
}
