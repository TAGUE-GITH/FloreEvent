package com.floreevent.image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.common.ApiException;

/**
 * Enregistre une image envoyee sous forme de « data URL ». L'interface la redimensionne avant l'envoi ;
 * le serveur verifie le type, la taille et que le contenu est vraiment une image (un faux fichier est refuse).
 */
@Service
public class ImageService {

	static final int MAX_BASE64_LENGTH = 400_000;
	private static final Map<String, String> TYPES = Map.of("image/png", "png", "image/jpeg", "jpeg");
	private static final Pattern DATA_URL = Pattern.compile("^data:(image/[a-z]+);base64,([A-Za-z0-9+/=]+)$");

	private final ImageRepository images;

	public ImageService(ImageRepository images) {
		this.images = images;
	}

	@Transactional
	public Image save(String dataUrl) {
		Matcher matcher = DATA_URL.matcher(dataUrl == null ? "" : dataUrl);
		if (!matcher.matches() || !TYPES.containsKey(matcher.group(1)))
			throw ApiException.badRequest("Formats acceptés : JPEG ou PNG.");
		String base64 = matcher.group(2);
		if (base64.length() > MAX_BASE64_LENGTH)
			throw ApiException.badRequest("Image trop lourde (300 Ko maximum après réduction).");
		try {
			byte[] bytes = Base64.getDecoder().decode(base64);
			if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) throw new IOException("illisible");
		} catch (IllegalArgumentException | IOException e) {
			throw ApiException.badRequest("Ce fichier n'est pas une image valide (JPEG ou PNG).");
		}
		return images.save(new Image(UUID.randomUUID().toString(), matcher.group(1), base64));
	}

	@Transactional
	public void delete(Image image) {
		if (image != null) images.delete(image);
	}

	@Transactional(readOnly = true)
	public Image get(String id) {
		return images.findById(id).orElseThrow(() -> ApiException.notFound("Image introuvable."));
	}
}
