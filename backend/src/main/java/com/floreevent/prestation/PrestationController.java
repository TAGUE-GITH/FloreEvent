package com.floreevent.prestation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.common.ApiException;
import com.floreevent.user.Role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** US-18 : prestations, visibles par tous sur le site, gerees par le gerant. */
@RestController
@Transactional
public class PrestationController {

	public record PrestationRequest(
			@NotBlank(message = "Indiquez le nom de la prestation.") @Size(max = 120, message = "120 caractères maximum.") String name,
			@Size(max = 1000, message = "1000 caractères maximum.") String description,
			@NotNull(message = "Indiquez le prix.") @PositiveOrZero(message = "Le prix ne peut pas être négatif.") Integer priceCents,
			Boolean active) {

		boolean isActive() {
			return active == null || active;
		}
	}

	public record PrestationResponse(Long id, String name, String description, int priceCents, boolean active) {

		public static PrestationResponse of(Prestation p) {
			return new PrestationResponse(p.getId(), p.getName(), p.getDescription(), p.getPriceCents(), p.isActive());
		}
	}

	private final PrestationRepository prestations;

	public PrestationController(PrestationRepository prestations) {
		this.prestations = prestations;
	}

	/** Site public : prestations proposees. */
	@GetMapping("/api/catalog/prestations")
	@Transactional(readOnly = true)
	public List<PrestationResponse> published() {
		return prestations.findByActiveTrueOrderByNameAsc().stream().map(PrestationResponse::of).toList();
	}

	@GetMapping("/api/prestations")
	@PreAuthorize(Role.STAFF)
	@Transactional(readOnly = true)
	public List<PrestationResponse> list() {
		return prestations.findAllByOrderByNameAsc().stream().map(PrestationResponse::of).toList();
	}

	@PostMapping("/api/prestations")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('GERANT')")
	public PrestationResponse create(@Valid @RequestBody PrestationRequest request) {
		String name = request.name().trim();
		if (prestations.existsByNameIgnoreCase(name)) throw ApiException.invalidField("name", "Cette prestation existe déjà.");
		return PrestationResponse.of(prestations.save(new Prestation(name, clean(request.description()), request.priceCents(), request.isActive())));
	}

	@PutMapping("/api/prestations/{id}")
	@PreAuthorize("hasRole('GERANT')")
	public PrestationResponse update(@PathVariable Long id, @Valid @RequestBody PrestationRequest request) {
		Prestation prestation = prestations.findById(id).orElseThrow(() -> ApiException.notFound("Prestation introuvable."));
		String name = request.name().trim();
		if (prestations.existsByNameIgnoreCaseAndIdNot(name, id)) throw ApiException.invalidField("name", "Cette prestation existe déjà.");
		prestation.update(name, clean(request.description()), request.priceCents(), request.isActive());
		return PrestationResponse.of(prestation);
	}

	private static String clean(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}
}
