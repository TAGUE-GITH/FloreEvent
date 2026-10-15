package com.floreevent.catalog;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.catalog.CatalogDtos.ItemRequest;
import com.floreevent.catalog.CatalogDtos.MaintenanceRequest;
import com.floreevent.common.ApiException;
import com.floreevent.common.Dates;
import com.floreevent.image.ImageService;
import com.floreevent.user.User;

/** US-03 et US-04 : categories, articles, photos et materiel en reparation. */
@Service
public class CatalogService {

	private final CategoryRepository categories;
	private final ItemRepository items;
	private final MaintenanceRepository maintenances;
	private final ItemImageRepository itemImages;
	private final ImageService images;

	public CatalogService(CategoryRepository categories, ItemRepository items, MaintenanceRepository maintenances,
			ItemImageRepository itemImages, ImageService images) {
		this.categories = categories;
		this.items = items;
		this.maintenances = maintenances;
		this.itemImages = itemImages;
		this.images = images;
	}

	// ---------- Categories ----------

	@Transactional
	public Category createCategory(String name) {
		String clean = name.trim();
		if (categories.existsByNameIgnoreCase(clean)) throw ApiException.conflict("La catégorie « " + clean + " » existe déjà.");
		return categories.save(new Category(clean));
	}

	@Transactional
	public Category renameCategory(Long id, String name) {
		Category category = category(id);
		String clean = name.trim();
		if (categories.existsByNameIgnoreCaseAndIdNot(clean, id))
			throw ApiException.conflict("La catégorie « " + clean + " » existe déjà.");
		category.setName(clean);
		return category;
	}

	@Transactional
	public void deleteCategory(Long id) {
		Category category = category(id);
		if (items.countByCategoryId(id) > 0)
			throw ApiException.conflict("Cette catégorie contient des articles : déplacez-les avant de la supprimer.");
		categories.delete(category);
	}

	public long itemCount(Long categoryId) {
		return items.countByCategoryId(categoryId);
	}

	private Category category(Long id) {
		return categories.findById(id).orElseThrow(() -> ApiException.badRequest("Catégorie introuvable."));
	}

	// ---------- Articles ----------

	@Transactional(readOnly = true)
	public List<Item> search(String q, Long categoryId) {
		String pattern = q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase() + "%";
		return items.search(pattern, categoryId == null, categoryId == null ? 0 : categoryId);
	}

	@Transactional(readOnly = true)
	public Item get(Long id) {
		return items.findById(id).orElseThrow(() -> ApiException.notFound("Article introuvable."));
	}

	@Transactional
	public Item create(ItemRequest request) {
		String reference = Item.normalize(request.reference());
		if (items.existsByReference(reference))
			throw ApiException.conflict("Un article existe déjà avec la référence " + reference + ".");
		Item item = new Item(reference, request.name().trim(), category(request.categoryId()));
		apply(item, request);
		return items.save(item);
	}

	@Transactional
	public Item update(Long id, ItemRequest request) {
		Item item = get(id);
		String reference = Item.normalize(request.reference());
		if (items.existsByReferenceAndIdNot(reference, id))
			throw ApiException.conflict("Un article existe déjà avec la référence " + reference + ".");
		item.setReference(reference);
		item.setName(request.name().trim());
		item.setCategory(category(request.categoryId()));
		apply(item, request);
		return item;
	}

	private static void apply(Item item, ItemRequest request) {
		item.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
		item.setDayPriceCents(request.dayPriceCents());
		item.setReplacementValueCents(request.replacementValueCents());
		item.setSetupPriceCents(request.setupPriceCents() == null ? 0 : request.setupPriceCents());
		item.setQuantity(request.quantity());
		if (request.active() != null) item.setActive(request.active());
	}

	@Transactional
	public Item addImage(Long id, String dataUrl) {
		Item item = get(id);
		item.addImage(images.save(dataUrl));
		return item;
	}

	@Transactional
	public Item makeMainImage(Long id, String imageId) {
		Item item = get(id);
		item.makeMain(imageId);
		return item;
	}

	@Transactional
	public Item removeImage(Long id, String imageId) {
		Item item = get(id);
		ItemImage photo = item.removeImage(imageId);
		// D'abord le lien article-photo, puis l'image elle-meme (sinon la base refuse la suppression)
		itemImages.delete(photo);
		itemImages.flush();
		images.delete(photo.getImage());
		return item;
	}

	/** Exemplaires en reparation aujourd'hui. */
	@Transactional(readOnly = true)
	public int inRepairToday(Long itemId) {
		LocalDate today = Dates.today();
		return maintenances.quantityInRepair(itemId, today, today);
	}

	// ---------- Materiel en reparation (R1) ----------

	/**
	 * L'article est verrouille pendant la verification : deux mises en reparation simultanees
	 * ne peuvent pas retirer plus d'exemplaires qu'il n'y en a en stock.
	 */
	@Transactional
	public Maintenance putInRepair(Long itemId, MaintenanceRequest request, User author) {
		if (request.toDate() != null && request.toDate().isBefore(request.fromDate()))
			throw ApiException.invalidField("toDate", "La date de fin doit être après la date de début.");
		Item item = items.findByIdForUpdate(itemId).orElseThrow(() -> ApiException.notFound("Article introuvable."));
		LocalDate until = request.toDate() == null ? LocalDate.of(9999, 12, 31) : request.toDate();
		int left = item.getQuantity() - maintenances.quantityInRepair(itemId, request.fromDate(), until);
		if (request.quantity() > left)
			throw ApiException.badRequest(Math.max(0, left) + " exemplaire(s) au plus peuvent encore être mis en réparation sur cette période.");
		String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
		return maintenances.save(new Maintenance(item, request.quantity(), request.fromDate(), request.toDate(), reason, author));
	}

	/** L'article est repare : disponible des aujourd'hui. Une reparation qui n'avait pas commence est simplement retiree. */
	@Transactional
	public Maintenance endRepair(Long id) {
		Maintenance maintenance = maintenances.findById(id)
				.orElseThrow(() -> ApiException.notFound("Réparation introuvable."));
		LocalDate today = Dates.today();
		if (!maintenance.getFromDate().isBefore(today)) {
			maintenances.delete(maintenance);
			return maintenance;
		}
		if (maintenance.getToDate() == null || !maintenance.getToDate().isBefore(today)) maintenance.endBefore(today);
		return maintenance;
	}

	@Transactional(readOnly = true)
	public List<Maintenance> openRepairs() {
		return maintenances.findOpen(Dates.today());
	}

	@Transactional(readOnly = true)
	public List<Maintenance> repairsOf(Long itemId) {
		get(itemId);
		return maintenances.findByItemIdOrderByFromDateDescIdDesc(itemId);
	}
}
