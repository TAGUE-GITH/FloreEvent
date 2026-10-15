package com.floreevent.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.common.Dates;

/** US-19 : chaque matin, rappel aux clients dont l'evenement commence dans quelques jours. */
@Component
public class ReminderJob {

	private final BookingRepository bookings;
	private final BookingEmails emails;
	private final int daysBefore;

	public ReminderJob(BookingRepository bookings, BookingEmails emails, @Value("${app.reminder.days-before}") int daysBefore) {
		this.bookings = bookings;
		this.emails = emails;
		this.daysBefore = daysBefore;
	}

	/** Renvoie le nombre de rappels envoyes. */
	@Scheduled(cron = "${app.reminder.cron}", zone = "Europe/Paris")
	@Transactional(readOnly = true)
	public int sendReminders() {
		var due = bookings.findByStatusAndStartDate(BookingStatus.CONFIRMEE, Dates.today().plusDays(daysBefore));
		due.forEach(emails::reminder);
		return due.size();
	}
}
