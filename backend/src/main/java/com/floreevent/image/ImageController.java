package com.floreevent.image;

import java.time.Duration;
import java.util.Base64;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Images publiques (photos de profil et de produits) : une image ne change jamais, elle se met en cache. */
@RestController
public class ImageController {

	private final ImageService images;

	public ImageController(ImageService images) {
		this.images = images;
	}

	@GetMapping("/api/images/{id}")
	public ResponseEntity<byte[]> image(@PathVariable String id) {
		Image image = images.get(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.getContentType()))
				.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
				.body(Base64.getDecoder().decode(image.getData()));
	}
}
