package com.floreevent.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailServiceTest {

	private final Mailer mailer = mock(Mailer.class);
	private final EmailService emails = new EmailService(mailer);

	@Test
	void emailsAreFormattedAndEscaped() {
		emails.send("lea@client.test", "Votre devis", "Bonjour Léa <b>,", List.of("Total : 600 €"), "Voir mon devis", "https://site/devis?a=1&b=2", null);

		var html = ArgumentCaptor.forClass(String.class);
		var text = ArgumentCaptor.forClass(String.class);
		verify(mailer).send(org.mockito.ArgumentMatchers.eq("lea@client.test"), org.mockito.ArgumentMatchers.eq("Votre devis"), html.capture(),
				text.capture());
		assertThat(html.getValue()).contains("FloreEvent", "Bonjour Léa &lt;b&gt;,", "href=\"https://site/devis?a=1&amp;b=2\"");
		assertThat(text.getValue()).contains("Total : 600 €", "Voir mon devis : https://site/devis?a=1&b=2");
	}

	@Test
	void demoAddressesNeverReceiveEmails() {
		emails.send("client@demo.floreevent.fr", "Votre devis", "Bonjour", List.of(), null, null, null);
		verify(mailer, never()).send(anyString(), anyString(), anyString(), anyString());
	}
}
