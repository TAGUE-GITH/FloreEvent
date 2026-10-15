package com.floreevent.mail;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

/**
 * Mise en forme des emails de FloreEvent (HTML et texte), confies ensuite au Mailer. Un email n'est envoye qu'une fois
 * l'operation enregistree en base : une operation annulee n'envoie rien.
 */
@Service
public class EmailService {

	/** Comptes de demonstration : adresses fictives, aucun email n'y est jamais envoye. */
	public static final String DEMO_DOMAIN = "@demo.floreevent.fr";
	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final Mailer mailer;

	public EmailService(Mailer mailer) {
		this.mailer = mailer;
	}

	/** Email simple : salutation, paragraphes, bouton facultatif (libelle et lien) et petite note. */
	public void send(String to, String subject, String greeting, List<String> paragraphs, String buttonLabel, String buttonUrl, String footnote) {
		if (to == null || to.isBlank()) return;
		var html = new StringBuilder("""
				<div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#1c1917">
				  <h2 style="color:#047857;margin-bottom:4px">FloreEvent</h2>
				  <p style="color:#78716c;margin-top:0;font-size:13px">Location de matériel pour vos événements · Melun</p>
				""");
		var text = new StringBuilder(greeting).append("\n\n");
		html.append("<p>").append(HtmlUtils.htmlEscape(greeting, "UTF-8")).append("</p>");
		for (String paragraph : paragraphs) {
			html.append("<p>").append(HtmlUtils.htmlEscape(paragraph, "UTF-8")).append("</p>");
			text.append(paragraph).append("\n");
		}
		if (buttonLabel != null) {
			html.append("""
					<p style="margin:28px 0"><a href="%s" style="background:#047857;color:#fff;padding:12px 20px;border-radius:10px;\
					text-decoration:none;font-weight:bold">%s</a></p>""".formatted(HtmlUtils.htmlEscape(buttonUrl, "UTF-8"), HtmlUtils.htmlEscape(buttonLabel, "UTF-8")));
			text.append("\n").append(buttonLabel).append(" : ").append(buttonUrl).append("\n");
		}
		if (footnote != null) {
			html.append("<p style=\"font-size:13px;color:#78716c\">").append(HtmlUtils.htmlEscape(footnote, "UTF-8")).append("</p>");
			text.append("\n").append(footnote);
		}
		html.append("</div>");
		dispatch(to.trim(), subject, html.toString(), text.toString());
	}

	private void dispatch(String to, String subject, String html, String text) {
		if (to.toLowerCase().endsWith(DEMO_DOMAIN)) {
			log.info("Email « {} » non envoyé : {} est une adresse de démonstration", subject, to);
			return;
		}
		Runnable sending = () -> mailer.send(to, subject, html, text);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					sending.run();
				}
			});
		} else sending.run();
	}
}
