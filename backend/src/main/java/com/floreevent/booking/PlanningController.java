package com.floreevent.booking;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.BookingDtos.PlanningTask;
import com.floreevent.user.Role;

/** US-14 : planning des livraisons et des retours, pour l'equipe. */
@RestController
@RequestMapping("/api/planning")
@PreAuthorize(Role.STAFF)
public class PlanningController {

	private final LogisticsService logistics;

	public PlanningController(LogisticsService logistics) {
		this.logistics = logistics;
	}

	@GetMapping
	public List<PlanningTask> planning(@RequestParam LocalDate from, @RequestParam LocalDate to) {
		return logistics.planning(from, to);
	}
}
