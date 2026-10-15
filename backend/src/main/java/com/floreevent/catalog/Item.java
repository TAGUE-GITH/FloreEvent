package com.floreevent.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.floreevent.common.ApiException;
import com.floreevent.image.Image;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Article loue : un barnum, une chaise, un lot de verres... La quantite est le stock total ;
 * les exemplaires en reparation ou deja reserves sont deduits pour une periode (regle R1).
 */
@Entity
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String reference;

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	private String description;

	/** Montants en centimes d'euro (R5). */
	@Column(name = "day_price_cents", nullable = false)
	private int dayPriceCents;

	@Column(name = "replacement_value_cents", nullable = false)
	private int replacementValueCents;

	@Column(name = "setup_price_cents", nullable = false)
	private int setupPriceCents;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<ItemImage> images = new ArrayList<>();

	protected Item() {
	}

	public Item(String reference, String name, Category category) {
		this.reference = normalize(reference);
		this.name = name;
		this.category = category;
	}

	/** References en majuscules, sans espaces autour : « barnum-3x6 » et « BARNUM-3X6 » sont la meme. */
	public static String normalize(String reference) {
		return reference.trim().toUpperCase();
	}

	public void addImage(Image image) {
		images.add(new ItemImage(this, image, images.size()));
	}

	/** Retire la photo de l'article et la renvoie (a supprimer avec son image) ; les positions restent continues. */
	public ItemImage removeImage(String imageId) {
		ItemImage photo = photo(imageId);
		images.remove(photo);
		renumber();
		return photo;
	}

	/** La photo choisie passe en premier (photo principale). */
	public void makeMain(String imageId) {
		ItemImage photo = photo(imageId);
		images.remove(photo);
		images.addFirst(photo);
		renumber();
	}

	private ItemImage photo(String imageId) {
		return images.stream().filter(p -> p.getImage().getId().equals(imageId)).findFirst()
				.orElseThrow(() -> ApiException.notFound("Photo introuvable."));
	}

	private void renumber() {
		for (int i = 0; i < images.size(); i++) images.get(i).setPosition(i);
	}

	public Image getMainImage() {
		return images.isEmpty() ? null : images.getFirst().getImage();
	}

	public Long getId() {
		return id;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = normalize(reference);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getDayPriceCents() {
		return dayPriceCents;
	}

	public void setDayPriceCents(int dayPriceCents) {
		this.dayPriceCents = dayPriceCents;
	}

	public int getReplacementValueCents() {
		return replacementValueCents;
	}

	public void setReplacementValueCents(int replacementValueCents) {
		this.replacementValueCents = replacementValueCents;
	}

	public int getSetupPriceCents() {
		return setupPriceCents;
	}

	public void setSetupPriceCents(int setupPriceCents) {
		this.setupPriceCents = setupPriceCents;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<ItemImage> getImages() {
		return images;
	}
}
