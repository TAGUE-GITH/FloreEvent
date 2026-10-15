package com.floreevent.booking;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.catalog.Item;
import com.floreevent.catalog.MaintenanceRepository;
import com.floreevent.common.ApiException;

/** R1 : exemplaires disponibles d'un article sur une periode (stock - reserve - en reparation). */
@Service
public class AvailabilityService {

	private final BookingRepository bookings;
	private final MaintenanceRepository maintenances;

	public AvailabilityService(BookingRepository bookings, MaintenanceRepository maintenances) {
		this.bookings = bookings;
		this.maintenances = maintenances;
	}

	public static void checkPeriod(LocalDate from, LocalDate to) {
		if (to.isBefore(from)) throw ApiException.badRequest("La date de fin doit être après la date de début.");
	}

	@Transactional(readOnly = true)
	public int available(Item item, LocalDate from, LocalDate to) {
		return available(item, from, to, -1);
	}

	/** excludedBookingId : la reservation qu'on confirme ne se compte pas elle-meme. */
	@Transactional(readOnly = true)
	public int available(Item item, LocalDate from, LocalDate to, long excludedBookingId) {
		int reserved = bookings.reservedQuantity(item.getId(), BookingStatus.BLOCKING, from.minusDays(1), to.plusDays(1), excludedBookingId);
		int inRepair = maintenances.quantityInRepair(item.getId(), from, to);
		return Math.max(0, item.getQuantity() - reserved - inRepair);
	}
}
