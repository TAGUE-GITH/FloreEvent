package com.floreevent.booking;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.catalog.Item;
import com.floreevent.catalog.ItemRepository;
import com.floreevent.catalog.MaintenanceRepository;
import com.floreevent.common.ApiException;

/** R1 : exemplaires disponibles d'un article sur une periode (stock - reserve - en reparation). */
@Service
public class AvailabilityService {

	private final BookingRepository bookings;
	private final MaintenanceRepository maintenances;
	private final ItemRepository items;

	public AvailabilityService(BookingRepository bookings, MaintenanceRepository maintenances, ItemRepository items) {
		this.bookings = bookings;
		this.maintenances = maintenances;
		this.items = items;
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

	/**
	 * Premier article qui manque pour confirmer cette reservation (message pour l'utilisateur), ou null si tout est libre.
	 * Avec lock (R2) : les articles sont verrouilles, dans l'ordre de leurs identifiants pour eviter les interblocages,
	 * jusqu'a la fin de la transaction qui confirme.
	 */
	@Transactional
	public String shortage(Booking booking, boolean lock) {
		List<BookingLine> lines = booking.getLines().stream().sorted(Comparator.comparing(l -> l.getItem().getId())).toList();
		for (BookingLine line : lines) {
			Item item = lock ? items.findByIdForUpdate(line.getItem().getId()).orElseThrow() : line.getItem();
			int available = available(item, booking.getStartDate(), booking.getEndDate(), booking.getId());
			if (line.getQuantity() > available) return line.getItemName() + " : " + available + " disponible(s) seulement à ces dates.";
		}
		return null;
	}
}
