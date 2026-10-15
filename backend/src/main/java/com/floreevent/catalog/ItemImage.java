package com.floreevent.catalog;

import com.floreevent.image.Image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Photo d'un article ; la position 0 est la photo principale. */
@Entity
@Table(name = "item_image")
public class ItemImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "item_id")
	private Item item;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "image_id")
	private Image image;

	@Column(nullable = false)
	private int position;

	protected ItemImage() {
	}

	ItemImage(Item item, Image image, int position) {
		this.item = item;
		this.image = image;
		this.position = position;
	}

	public Image getImage() {
		return image;
	}

	public int getPosition() {
		return position;
	}

	void setPosition(int position) {
		this.position = position;
	}
}
