package com.floreevent.dashboard;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.Booking;
import com.floreevent.booking.BookingLine;
import com.floreevent.booking.BookingRepository;
import com.floreevent.booking.BookingStatus;
import com.floreevent.booking.DeliveryMode;
import com.floreevent.booking.Payment;
import com.floreevent.common.Dates;
import com.floreevent.contact.ContactMessageRepository;
import com.floreevent.user.Role;

/** US-20 : l'activite en un coup d'oeil (chiffre d'affaires, reservations a venir, cautions, articles les plus loues). */
@RestController
@PreAuthorize(Role.STAFF)
public class DashboardController {

	public record MonthRevenue(String month, String label, int cents) {
	}

	public record TopItem(String name, int quantity) {
	}

	public record NextBooking(Long id, String reference, String customer, LocalDate startDate, DeliveryMode deliveryMode, BookingStatus status) {
	}

	public record Dashboard(int revenueThisMonthCents, List<MonthRevenue> revenueByMonth, long pendingRequests, long quotesAwaiting,
			long upcomingBookings, long depositsHeld, int depositsHeldCents, int balanceDueCents, long unhandledMessages,
			List<TopItem> topItems, List<NextBooking> nextBookings) {
	}

	private static final List<BookingStatus> RENTED = List.of(BookingStatus.CONFIRMEE, BookingStatus.EN_COURS, BookingStatus.RETOURNEE,
			BookingStatus.CLOTUREE);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);

	private final BookingRepository bookings;
	private final ContactMessageRepository messages;

	public DashboardController(BookingRepository bookings, ContactMessageRepository messages) {
		this.bookings = bookings;
		this.messages = messages;
	}

	@GetMapping("/api/dashboard")
	@Transactional(readOnly = true)
	public Dashboard dashboard() {
		LocalDate today = Dates.today();
		List<Booking> all = bookings.findAll();

		// Encaissements des 6 derniers mois (remboursements deduits), mois en cours compris
		YearMonth current = YearMonth.from(today);
		Map<YearMonth, Integer> revenue = new LinkedHashMap<>();
		for (int i = 5; i >= 0; i--) revenue.put(current.minusMonths(i), 0);
		for (Booking b : all)
			for (Payment p : b.getPayments()) {
				if (p.getPaidAt() == null) continue;
				YearMonth month = YearMonth.from(p.getPaidAt().atZone(Dates.ZONE));
				revenue.computeIfPresent(month, (m, cents) -> cents + p.getNetCents());
			}
		List<MonthRevenue> byMonth = revenue.entrySet().stream()
				.map(e -> new MonthRevenue(e.getKey().toString(), e.getKey().format(MONTH), e.getValue())).toList();

		Map<String, Integer> rented = new LinkedHashMap<>();
		for (Booking b : all)
			if (RENTED.contains(b.getStatus())) for (BookingLine l : b.getLines()) rented.merge(l.getItemName(), l.getQuantity(), Integer::sum);
		List<TopItem> top = rented.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
				.map(e -> new TopItem(e.getKey(), e.getValue())).toList();

		List<Booking> held = all.stream().filter(b -> b.getStatus() == BookingStatus.EN_COURS).toList();
		int balanceDue = all.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMEE || b.getStatus() == BookingStatus.EN_COURS
				|| b.getStatus() == BookingStatus.RETOURNEE).mapToInt(b -> Math.max(0, b.getBalanceCents())).sum();
		List<NextBooking> next = new ArrayList<>(all.stream()
				.filter(b -> (b.getStatus() == BookingStatus.CONFIRMEE || b.getStatus() == BookingStatus.EN_COURS) && !b.getEndDate().isBefore(today))
				.sorted(Comparator.comparing(Booking::getStartDate)).limit(5)
				.map(b -> new NextBooking(b.getId(), b.getReference(), b.getCustomer().getFullName(), b.getStartDate(), b.getDeliveryMode(),
						b.getStatus()))
				.toList());

		return new Dashboard(revenue.get(current), byMonth, count(all, BookingStatus.DEMANDE), count(all, BookingStatus.DEVIS),
				all.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMEE && !b.getStartDate().isAfter(today.plusDays(30))).count(),
				held.size(), held.stream().mapToInt(Booking::getSecurityDepositCents).sum(), balanceDue, messages.countByHandledFalse(), top,
				next);
	}

	private static long count(List<Booking> all, BookingStatus status) {
		return all.stream().filter(b -> b.getStatus() == status).count();
	}
}
