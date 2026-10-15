package com.floreevent.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.floreevent.common.ApiException;

/** Calcul du prix d'une location (hypotheses du cahier des charges, toutes parametrables). */
class PricingTest {

	private final Pricing pricing = new Pricing(50, List.of(new Pricing.Zone(15, 4000), new Pricing.Zone(30, 7000), new Pricing.Zone(50, 11000)),
			30, 30, 15000);

	@Test
	void eachExtraDayCostsHalfTheDailyPrice() {
		assertThat(pricing.rental(15000, 1, 1)).isEqualTo(15000);
		assertThat(pricing.rental(15000, 3, 1)).isEqualTo(30000);
		assertThat(pricing.rental(350, 2, 120)).isEqualTo(63000);
		// Arrondi au centime le plus proche : 1,25 € sur 2 jours = 1,875 € → 1,88 €
		assertThat(pricing.rental(125, 2, 1)).isEqualTo(188);
	}

	@Test
	void theDeliveryPriceDependsOnTheZone() {
		assertThat(pricing.delivery(3)).isEqualTo(4000);
		assertThat(pricing.delivery(15)).isEqualTo(4000);
		assertThat(pricing.delivery(16)).isEqualTo(7000);
		assertThat(pricing.delivery(50)).isEqualTo(11000);
		assertThatThrownBy(() -> pricing.delivery(51)).isInstanceOf(ApiException.class)
				.hasMessage("Au-delà de 50 km, contactez-nous pour un devis personnalisé.");
	}

	@Test
	void depositAndSecurityDepositFollowTheRules() {
		assertThat(pricing.deposit(106000)).isEqualTo(31800);
		// Caution : 30 % de la valeur du materiel, au moins 150 €
		assertThat(pricing.securityDeposit(480000)).isEqualTo(144000);
		assertThat(pricing.securityDeposit(20000)).isEqualTo(15000);
	}

	@Test
	void pricesIncludeTwentyPercentVat() {
		assertThat(Pricing.vatIncluded(12000)).isEqualTo(2000);
		assertThat(Pricing.vatIncluded(100)).isEqualTo(17);
	}

	@Test
	void theDistanceIsTheStraightLineIncreasedForTheRoads() {
		// Melun → Fontainebleau : environ 14 km a vol d'oiseau, environ 18 km par la route
		double km = Pricing.roadKm(48.53618, 2.658187, 48.4047, 2.7016);
		assertThat(km).isBetween(17.0, 20.0);
	}
}
