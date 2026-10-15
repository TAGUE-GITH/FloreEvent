package com.floreevent.quote;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Les regles de prix viennent de la configuration : FloreEvent peut les changer sans toucher au code. */
@Configuration
public class PricingConfig {

	@Bean
	public Pricing pricing(@Value("${app.pricing.extra-day-percent}") int extraDayPercent,
			@Value("${app.pricing.delivery-zones}") String zones, @Value("${app.pricing.deposit-percent}") int depositPercent,
			@Value("${app.pricing.security-deposit-percent}") int securityDepositPercent,
			@Value("${app.pricing.min-security-deposit-cents}") int minSecurityDepositCents) {
		// « 15:4000,30:7000,50:11000 » : jusqu'a 15 km 40 €, jusqu'a 30 km 70 €, jusqu'a 50 km 110 €
		List<Pricing.Zone> parsed = Arrays.stream(zones.split(","))
				.map(zone -> zone.trim().split(":"))
				.map(parts -> new Pricing.Zone(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])))
				.toList();
		return new Pricing(extraDayPercent, parsed, depositPercent, securityDepositPercent, minSecurityDepositCents);
	}
}
