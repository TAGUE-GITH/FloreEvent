package com.floreevent.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Coordonnees de FloreEvent imprimees sur les documents (saisies chez l'hebergeur, a completer par l'entreprise). */
@Component
public record CompanyInfo(@Value("${app.company.name}") String name, @Value("${app.company.address}") String address,
		@Value("${app.company.phone}") String phone, @Value("${app.company.email}") String email,
		@Value("${app.company.siret}") String siret) {

	String siretLine() {
		return siret == null || siret.isBlank() ? "SIRET : à compléter" : "SIRET : " + siret;
	}
}
