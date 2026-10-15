package com.floreevent.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sans cle Brevo (developpement, tests) : l'email est ecrit dans le journal du serveur au lieu d'etre envoye. */
public class LoggingMailer implements Mailer {

	private static final Logger log = LoggerFactory.getLogger(LoggingMailer.class);

	@Override
	public void send(String to, String subject, String htmlContent, String textContent) {
		log.info("Email pour {} - {}\n{}", to, subject, textContent);
	}
}
