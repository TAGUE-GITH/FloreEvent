package com.floreevent.mail;

/** Envoi d'un email. Une interface permet de changer de fournisseur (Brevo, journal...) sans toucher au reste du code. */
public interface Mailer {

	void send(String to, String subject, String htmlContent, String textContent);
}
