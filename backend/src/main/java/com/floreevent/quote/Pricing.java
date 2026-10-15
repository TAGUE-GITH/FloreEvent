package com.floreevent.quote;

import java.util.List;

import com.floreevent.common.ApiException;

/**
 * Regles de prix de FloreEvent (hypotheses du cahier des charges, toutes parametrables dans application.properties).
 * Montants TTC en centimes d'euro (R5).
 */
public class Pricing {

	/** Zone de livraison : jusqu'a maxKm kilometres de Melun, au prix priceCents. */
	public record Zone(int maxKm, int priceCents) {
	}

	/** Les distances a vol d'oiseau sont majorees pour approcher la distance par la route. */
	static final double ROAD_FACTOR = 1.3;
	private static final double EARTH_RADIUS_KM = 6371;

	private final int extraDayPercent;
	private final List<Zone> zones;
	private final int depositPercent;
	private final int securityDepositPercent;
	private final int minSecurityDepositCents;

	public Pricing(int extraDayPercent, List<Zone> zones, int depositPercent, int securityDepositPercent, int minSecurityDepositCents) {
		this.extraDayPercent = extraDayPercent;
		this.zones = zones;
		this.depositPercent = depositPercent;
		this.securityDepositPercent = securityDepositPercent;
		this.minSecurityDepositCents = minSecurityDepositCents;
	}

	/** Location d'une ligne : le premier jour au tarif plein, chaque jour suivant a extraDayPercent % du tarif. */
	public int rental(int dayPriceCents, int days, int quantity) {
		return (int) Math.round((double) dayPriceCents * quantity * (100 + (days - 1) * extraDayPercent) / 100);
	}

	public int delivery(int distanceKm) {
		return zones.stream().filter(zone -> distanceKm <= zone.maxKm()).findFirst().map(Zone::priceCents)
				.orElseThrow(() -> ApiException.invalidField("address",
						"Au-delà de " + zones.getLast().maxKm() + " km, contactez-nous pour un devis personnalisé."));
	}

	/** Acompte paye a la reservation. */
	public int deposit(int totalCents) {
		return (int) Math.round(totalCents * depositPercent / 100.0);
	}

	/** Caution : un pourcentage de la valeur de remplacement du materiel loue, avec un minimum. */
	public int securityDeposit(long replacementValueCents) {
		return (int) Math.max(minSecurityDepositCents, Math.round(replacementValueCents * securityDepositPercent / 100.0));
	}

	/** TVA a 20 % contenue dans un prix TTC. */
	public static int vatIncluded(int totalCents) {
		return totalCents - (int) Math.round(totalCents / 1.2);
	}

	/** Distance estimee par la route entre deux points GPS (formule de haversine, majoree). */
	public static double roadKm(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a)) * ROAD_FACTOR;
	}
}
