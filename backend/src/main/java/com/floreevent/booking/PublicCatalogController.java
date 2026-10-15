package com.floreevent.booking;

import java.time.LocalDate;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.booking.BookingDtos.BookingRequest;
import com.floreevent.booking.BookingDtos.EstimateResponse;
import com.floreevent.booking.BookingDtos.PublicItem;
import com.floreevent.catalog.CatalogDtos.Named;
import com.floreevent.catalog.CatalogService;
import com.floreevent.catalog.Item;
import com.floreevent.common.ApiException;

import jakarta.validation.Valid;

/** US-05 et US-07 : catalogue public (sans connexion) avec les disponibilites, et estimation du prix. */
@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class PublicCatalogController {

	private final CatalogService catalog;
	private final AvailabilityService availability;
	private final BookingService bookings;

	public PublicCatalogController(CatalogService catalog, AvailabilityService availability, BookingService bookings) {
		this.catalog = catalog;
		this.availability = availability;
		this.bookings = bookings;
	}

	/** Disponibilite affichee seulement quand les deux dates sont choisies. */
	private PublicItem publicItem(Item item, LocalDate from, LocalDate to) {
		return PublicItem.of(item, from == null || to == null ? null : availability.available(item, from, to));
	}

	@GetMapping("/catalog/items")
	public List<PublicItem> items(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
		if (from != null && to != null) AvailabilityService.checkPeriod(from, to);
		return catalog.search(q, categoryId).stream().filter(Item::isActive).map(item -> publicItem(item, from, to)).toList();
	}

	@GetMapping("/catalog/items/{id}")
	public PublicItem item(@PathVariable Long id, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		if (from != null && to != null) AvailabilityService.checkPeriod(from, to);
		Item item = catalog.search(null, null).stream().filter(i -> i.getId().equals(id) && i.isActive()).findFirst()
				.orElseThrow(() -> ApiException.notFound("Cet article n'est plus proposé à la location."));
		return publicItem(item, from, to);
	}

	/** Categories qui ont au moins un article propose a la location. */
	@GetMapping("/catalog/categories")
	public List<Named> categories() {
		return catalog.search(null, null).stream().filter(Item::isActive).map(Item::getCategory).distinct()
				.map(category -> new Named(category.getId(), category.getName())).toList();
	}

	@PostMapping("/quote/estimate")
	public EstimateResponse estimate(@Valid @RequestBody BookingRequest request) {
		return EstimateResponse.of(bookings.estimate(request));
	}
}
