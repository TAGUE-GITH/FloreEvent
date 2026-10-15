package com.floreevent.user;

/** Un membre de l'equipe a un seul role ; le gerant peut tout faire. Les clients du site ont le role CLIENT. */
public enum Role {
	GERANT,
	COMMERCIAL,
	LOGISTIQUE,
	CLIENT;

	/** Expression de securite des pages reservees a l'equipe (jamais aux clients). */
	public static final String STAFF = "hasAnyRole('GERANT', 'COMMERCIAL', 'LOGISTIQUE')";
}
