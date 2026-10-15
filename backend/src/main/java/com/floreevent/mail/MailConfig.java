package com.floreevent.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Avec une cle Brevo : vrais emails. Sans cle : emails ecrits dans le journal du serveur. */
@Configuration
public class MailConfig {

	private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

	@Bean
	public Mailer mailer(@Value("${app.mail.brevo-api-key}") String apiKey,
			@Value("${app.mail.sender-email}") String senderEmail, @Value("${app.mail.sender-name}") String senderName) {
		if (apiKey.isBlank()) {
			log.warn("BREVO_API_KEY absente : les emails seront écrits dans le journal du serveur");
			return new LoggingMailer();
		}
		return new BrevoMailer(RestClient.builder(), apiKey, senderEmail, senderName);
	}
}
