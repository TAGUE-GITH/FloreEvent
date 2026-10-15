package com.floreevent.image;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Image envoyee par un utilisateur (photo de profil, photo de produit), stockee en base64 :
 * le disque des hebergeurs gratuits est efface a chaque redeploiement. L'identifiant aleatoire sert d'adresse publique.
 */
@Entity
public class Image {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "content_type", nullable = false)
	private String contentType;

	@Column(nullable = false, length = 400_000)
	private String data;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Image() {
	}

	public Image(String id, String contentType, String data) {
		this.id = id;
		this.contentType = contentType;
		this.data = data;
	}

	/** Adresse publique de l'image (null si pas d'image). */
	public static String url(Image image) {
		return image == null ? null : "/api/images/" + image.getId();
	}

	public String getId() {
		return id;
	}

	public String getContentType() {
		return contentType;
	}

	public String getData() {
		return data;
	}
}
