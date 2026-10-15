package com.floreevent.catalog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.floreevent.image.Image;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Donnees echangees avec l'API pour le catalogue. Les montants sont en centimes d'euro (R5). */
public final class CatalogDtos {

	private CatalogDtos() {
	}

	public record CategoryRequest(@NotBlank(message = "Le nom est obligatoire.") @Size(max = 80) String name) {
	}

	public record CategoryResponse(Long id, String name, long itemCount) {
	}

	public record ItemRequest(
			@NotBlank(message = "La référence est obligatoire.") @Size(max = 40) String reference,
			@NotBlank(message = "Le nom est obligatoire.") @Size(max = 160) String name,
			@NotNull(message = "Choisissez une catégorie.") Long categoryId,
			@Size(max = 2000) String description,
			@NotNull(message = "Le prix par jour est obligatoire.") @Positive(message = "Le prix par jour doit être supérieur à 0.") Integer dayPriceCents,
			@NotNull(message = "La valeur de remplacement est obligatoire.") @PositiveOrZero(message = "La valeur de remplacement ne peut pas être négative.") Integer replacementValueCents,
			@PositiveOrZero(message = "Le prix d'installation ne peut pas être négatif.") Integer setupPriceCents,
			@NotNull(message = "La quantité est obligatoire.") @Min(value = 0, message = "La quantité ne peut pas être négative.") Integer quantity,
			Boolean active) {
	}

	public record Named(Long id, String name) {
	}

	public record Photo(String id, String url) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ItemResponse(Long id, String reference, String name, Named category, String description,
			int dayPriceCents, int replacementValueCents, int setupPriceCents, int quantity, int inRepair, boolean active,
			String imageUrl, List<Photo> images) {

		/** inRepair : exemplaires en reparation aujourd'hui. */
		public static ItemResponse of(Item item, int inRepair) {
			return new ItemResponse(item.getId(), item.getReference(), item.getName(),
					new Named(item.getCategory().getId(), item.getCategory().getName()), item.getDescription(),
					item.getDayPriceCents(), item.getReplacementValueCents(), item.getSetupPriceCents(), item.getQuantity(),
					inRepair, item.isActive(), Image.url(item.getMainImage()),
					item.getImages().stream().map(p -> new Photo(p.getImage().getId(), Image.url(p.getImage()))).toList());
		}
	}

	public record ImageRequest(@NotBlank(message = "Choisissez une photo.") String dataUrl) {
	}

	public record MaintenanceRequest(
			@NotNull(message = "Indiquez la quantité.") @Positive(message = "La quantité doit être d'au moins 1.") Integer quantity,
			@NotNull(message = "Indiquez la date de début.") LocalDate fromDate,
			LocalDate toDate,
			@Size(max = 255) String reason) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record MaintenanceResponse(Long id, Long itemId, String itemName, int quantity, LocalDate fromDate,
			LocalDate toDate, String reason, String author, Instant createdAt) {

		public static MaintenanceResponse of(Maintenance m) {
			return new MaintenanceResponse(m.getId(), m.getItem().getId(), m.getItem().getName(), m.getQuantity(),
					m.getFromDate(), m.getToDate(), m.getReason(), m.getAuthor().getFullName(), m.getCreatedAt());
		}
	}
}
