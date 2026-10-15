package com.floreevent.quote;

import java.util.Optional;

/** Localise une adresse en France (coordonnees GPS) pour calculer la distance de livraison depuis Melun. */
public interface Geocoder {

	/** label : adresse normalisee (« 5 Rue Grande 77300 Fontainebleau »). */
	record Location(String label, double latitude, double longitude) {
	}

	Optional<Location> locate(String address);
}
