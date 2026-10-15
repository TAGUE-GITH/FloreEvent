package com.floreevent.quote;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Service public et gratuit de geocodage de l'IGN (Geoplateforme, base adresse nationale), qui remplace l'API Adresse.
 * Seules les adresses assez sures (score >= 0,5) sont retenues.
 */
@Component
public class GeoplateformeGeocoder implements Geocoder {

	private static final Logger log = LoggerFactory.getLogger(GeoplateformeGeocoder.class);
	private static final double MIN_SCORE = 0.5;

	private final RestClient http = RestClient.create("https://data.geopf.fr/geocodage");

	@Override
	@SuppressWarnings("unchecked")
	public Optional<Location> locate(String address) {
		try {
			Map<String, Object> body = http.get().uri(uri -> uri.path("/search").queryParam("q", address).queryParam("limit", 1).build())
					.retrieve().body(Map.class);
			List<Map<String, Object>> features = body == null ? List.of() : (List<Map<String, Object>>) body.get("features");
			if (features == null || features.isEmpty()) return Optional.empty();
			Map<String, Object> feature = features.getFirst();
			Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
			if (((Number) properties.get("score")).doubleValue() < MIN_SCORE) return Optional.empty();
			List<Number> coordinates = (List<Number>) ((Map<String, Object>) feature.get("geometry")).get("coordinates");
			return Optional.of(new Location((String) properties.get("label"), coordinates.get(1).doubleValue(), coordinates.get(0).doubleValue()));
		} catch (RestClientException e) {
			log.warn("Géocodage impossible pour « {} » : {}", address, e.getMessage());
			return Optional.empty();
		}
	}
}
