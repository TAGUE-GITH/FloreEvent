package com.floreevent.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoneyTest {

	@Test
	void amountsAreFormattedTheFrenchWay() {
		assertThat(Money.euros(153000)).isEqualTo("1 530,00 €");
		assertThat(Money.euros(5)).isEqualTo("0,05 €");
		assertThat(Money.euros(-9000)).isEqualTo("-90,00 €");
		assertThat(Money.euros(123456789)).isEqualTo("1 234 567,89 €");
	}
}
