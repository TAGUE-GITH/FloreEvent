package com.floreevent.catalog;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.catalog.CatalogDtos.MaintenanceResponse;
import com.floreevent.user.Role;

/** US-04 : materiel en reparation (en cours ou prevu), et fin de reparation. */
@RestController
@RequestMapping("/api/maintenance")
@PreAuthorize(Role.STAFF)
@Transactional
public class MaintenanceController {

	private final CatalogService catalog;

	public MaintenanceController(CatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public List<MaintenanceResponse> open() {
		return catalog.openRepairs().stream().map(MaintenanceResponse::of).toList();
	}

	@PostMapping("/{id}/end")
	@PreAuthorize("hasAnyRole('GERANT', 'LOGISTIQUE')")
	public MaintenanceResponse end(@PathVariable Long id) {
		return MaintenanceResponse.of(catalog.endRepair(id));
	}
}
