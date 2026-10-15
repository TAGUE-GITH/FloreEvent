package com.floreevent.catalog;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.catalog.CatalogDtos.ImageRequest;
import com.floreevent.catalog.CatalogDtos.ItemRequest;
import com.floreevent.catalog.CatalogDtos.ItemResponse;
import com.floreevent.catalog.CatalogDtos.MaintenanceRequest;
import com.floreevent.catalog.CatalogDtos.MaintenanceResponse;
import com.floreevent.user.Role;
import com.floreevent.user.User;

import jakarta.validation.Valid;

/**
 * US-03 : articles du catalogue (consultes par toute l'equipe, modifies par le gerant).
 * US-04 : la logistique et le gerant mettent du materiel en reparation.
 */
@RestController
@RequestMapping("/api/items")
@PreAuthorize(Role.STAFF)
@Transactional
public class ItemController {

	private final CatalogService catalog;

	public ItemController(CatalogService catalog) {
		this.catalog = catalog;
	}

	private ItemResponse response(Item item) {
		return ItemResponse.of(item, catalog.inRepairToday(item.getId()));
	}

	@GetMapping
	public List<ItemResponse> list(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId) {
		return catalog.search(q, categoryId).stream().map(this::response).toList();
	}

	@GetMapping("/{id}")
	public ItemResponse get(@PathVariable Long id) {
		return response(catalog.get(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('GERANT')")
	public ItemResponse create(@Valid @RequestBody ItemRequest request) {
		return response(catalog.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('GERANT')")
	public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
		return response(catalog.update(id, request));
	}

	@PostMapping("/{id}/images")
	@PreAuthorize("hasRole('GERANT')")
	public ItemResponse addImage(@PathVariable Long id, @Valid @RequestBody ImageRequest request) {
		return response(catalog.addImage(id, request.dataUrl()));
	}

	@PostMapping("/{id}/images/{imageId}/main")
	@PreAuthorize("hasRole('GERANT')")
	public ItemResponse makeMain(@PathVariable Long id, @PathVariable String imageId) {
		return response(catalog.makeMainImage(id, imageId));
	}

	@DeleteMapping("/{id}/images/{imageId}")
	@PreAuthorize("hasRole('GERANT')")
	public ItemResponse removeImage(@PathVariable Long id, @PathVariable String imageId) {
		return response(catalog.removeImage(id, imageId));
	}

	@GetMapping("/{id}/maintenance")
	public List<MaintenanceResponse> repairs(@PathVariable Long id) {
		return catalog.repairsOf(id).stream().map(MaintenanceResponse::of).toList();
	}

	@PostMapping("/{id}/maintenance")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('GERANT', 'LOGISTIQUE')")
	public MaintenanceResponse putInRepair(@PathVariable Long id, @Valid @RequestBody MaintenanceRequest request,
			@AuthenticationPrincipal User user) {
		return MaintenanceResponse.of(catalog.putInRepair(id, request, user));
	}
}
