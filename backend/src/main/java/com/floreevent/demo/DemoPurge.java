package com.floreevent.demo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floreevent.mail.EmailService;

/**
 * Passage en mode reel (APP_DEMO=false) : les donnees de demonstration sont supprimees au demarrage. Les comptes d'essai
 * (mot de passe public) disparaissent, ainsi que les faux clients, leurs reservations, le materiel, les prestations et
 * les messages fictifs. Les vraies donnees ne sont jamais touchees : un article ou une prestation deja utilise par une vraie
 * reservation est garde.
 */
@Service
public class DemoPurge {

	private static final Logger log = LoggerFactory.getLogger(DemoPurge.class);
	private static final String DEMO = "%" + EmailService.DEMO_DOMAIN;
	private static final String DEMO_BOOKINGS = "select b.id from booking b join customer c on c.id = b.customer_id where lower(c.email) like ?";
	private static final String DEMO_USERS = "select id from app_user where lower(email) like ?";

	private final JdbcTemplate jdbc;

	public DemoPurge(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Renvoie false s'il n'y avait aucune donnee de demonstration. */
	@Transactional
	public boolean purge() {
		Integer accounts = jdbc.queryForObject("select count(*) from app_user where lower(email) like ?", Integer.class, DEMO);
		if (accounts == null || accounts == 0) return false;

		// Reservations des faux clients, avec leurs paiements, factures et historique
		for (String table : List.of("invoice", "payment", "booking_event", "booking_line", "booking_prestation"))
			jdbc.update("delete from " + table + " where booking_id in (" + DEMO_BOOKINGS + ")", DEMO);
		jdbc.update("delete from booking where id in (" + DEMO_BOOKINGS + ")", DEMO);

		// Comptes d'essai : leurs traces dans les vraies donnees restent, sans auteur
		jdbc.update("update booking_event set author_id = null where author_id in (" + DEMO_USERS + ")", DEMO);
		jdbc.update("update payment set author_id = null where author_id in (" + DEMO_USERS + ")", DEMO);
		jdbc.update("delete from maintenance where author_id in (" + DEMO_USERS + ")", DEMO);
		jdbc.update("delete from password_reset_token where user_id in (" + DEMO_USERS + ")", DEMO);
		jdbc.update("delete from app_user where lower(email) like ?", DEMO);
		jdbc.update("delete from customer where lower(email) like ? and id not in (select customer_id from booking)", DEMO);

		// Materiel, prestations et categories de la demonstration, s'ils ne servent a aucune vraie reservation
		for (String reference : DemoService.ITEM_REFERENCES) {
			List<Long> ids = jdbc.queryForList("select id from item where reference = ? and id not in (select item_id from booking_line)", Long.class,
					reference);
			for (Long id : ids) {
				List<String> images = jdbc.queryForList("select image_id from item_image where item_id = ?", String.class, id);
				jdbc.update("delete from maintenance where item_id = ?", id);
				jdbc.update("delete from item_image where item_id = ?", id);
				jdbc.update("delete from item where id = ?", id);
				for (String image : images) jdbc.update("delete from image where id = ?", image);
			}
		}
		for (String name : DemoService.PRESTATION_NAMES)
			jdbc.update("delete from prestation where name = ? and id not in (select prestation_id from booking_prestation)", name);
		for (String name : DemoService.CATEGORY_NAMES)
			jdbc.update("delete from category where name = ? and id not in (select category_id from item)", name);
		jdbc.update("delete from contact_message where lower(email) like ?", DEMO);

		// Plus aucune facture : la numerotation de la vraie entreprise repart de FAC-annee-00001 (R6)
		Integer invoices = jdbc.queryForObject("select count(*) from invoice", Integer.class);
		if (invoices != null && invoices == 0) jdbc.update("delete from invoice_counter");
		log.info("Mode réel : données de démonstration supprimées ({} comptes d'essai)", accounts);
		return true;
	}
}
