package com.floreevent.demo;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.floreevent.booking.Booking;
import com.floreevent.booking.BookingDtos.BookingRequest;
import com.floreevent.booking.BookingDtos.LineRequest;
import com.floreevent.booking.BookingDtos.OfflinePaymentRequest;
import com.floreevent.booking.BookingDtos.QuoteRequest;
import com.floreevent.booking.BookingDtos.ReturnLine;
import com.floreevent.booking.BookingDtos.ReturnRequest;
import com.floreevent.booking.BookingLine;
import com.floreevent.booking.BookingService;
import com.floreevent.booking.DeliveryMode;
import com.floreevent.booking.LogisticsService;
import com.floreevent.booking.PaymentMethod;
import com.floreevent.booking.PaymentService;
import com.floreevent.catalog.CatalogDtos.ItemRequest;
import com.floreevent.catalog.CatalogDtos.MaintenanceRequest;
import com.floreevent.catalog.CatalogService;
import com.floreevent.catalog.Item;
import com.floreevent.common.Dates;
import com.floreevent.common.Terms;
import com.floreevent.contact.ContactMessage;
import com.floreevent.contact.ContactMessageRepository;
import com.floreevent.customer.Customer;
import com.floreevent.customer.CustomerRepository;
import com.floreevent.mail.EmailService;
import com.floreevent.prestation.Prestation;
import com.floreevent.prestation.PrestationRepository;
import com.floreevent.user.Role;
import com.floreevent.user.User;
import com.floreevent.user.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * US-23 : donnees de demonstration entierement fictives (noms, adresses, reservations) pour presenter l'application
 * sans montrer les vraies donnees de FloreEvent. Tout passe par les services : les regles metier restent respectees.
 */
@Service
public class DemoService {

	/** Mot de passe public des comptes de demonstration (affiche sur les pages de connexion). */
	public static final String PASSWORD = "demo-floreevent-2026";

	public record DemoAccount(String email, Role role, String firstName, String lastName) {
	}

	public static final List<DemoAccount> ACCOUNTS = List.of(
			new DemoAccount("gerant" + EmailService.DEMO_DOMAIN, Role.GERANT, "Claire", "Martin"),
			new DemoAccount("commercial" + EmailService.DEMO_DOMAIN, Role.COMMERCIAL, "Julien", "Petit"),
			new DemoAccount("logistique" + EmailService.DEMO_DOMAIN, Role.LOGISTIQUE, "Karim", "Benali"),
			new DemoAccount("client" + EmailService.DEMO_DOMAIN, Role.CLIENT, "Sophie", "Laurent"));

	/** Article : reference, nom, categorie, prix/jour, valeur de remplacement, installation, stock, photo Unsplash, description. */
	private record DemoItem(String reference, String name, String category, int dayPrice, int replacement, int setup, int quantity,
			String photo, String description) {
	}

	private static final List<DemoItem> ITEMS = List.of(
			new DemoItem("BARNUM-3X6", "Barnum 3 x 6 m", "Tentes et barnums", 15000, 120000, 8000, 6, "1618106494700-4b0049e83ed8",
					"Structure aluminium et bâche blanche ignifugée, avec murs latéraux. Idéal pour 30 personnes debout."),
			new DemoItem("TENTE-6X12", "Tente de réception 6 x 12 m", "Tentes et barnums", 45000, 600000, 25000, 2, "1675376616537-c8aa9ddc9977",
					"Grande tente de réception pour 80 invités assis, plancher en option. Montage par notre équipe."),
			new DemoItem("CHAISE-NAP", "Chaise Napoléon blanche", "Mobilier", 350, 4500, 0, 300, "1559982240-f760db87b822",
					"Chaise élégante en résine, coussin blanc. Parfaite pour les mariages."),
			new DemoItem("TABLE-RONDE", "Table ronde 10 personnes", "Mobilier", 1200, 15000, 0, 40, "1751891159950-bf33e1e8705b",
					"Diamètre 180 cm, plateau bois, pliante. Nappe disponible séparément."),
			new DemoItem("TABLE-BOIS", "Table en bois massif 8 personnes", "Mobilier", 2500, 30000, 0, 10, "1647296020388-787fdae78e3c",
					"Table champêtre 220 x 90 cm, bois naturel, pour des réceptions chaleureuses."),
			new DemoItem("ASSIETTES-10", "Lot de 10 assiettes en porcelaine", "Art de la table", 800, 5000, 0, 60, "1560145726-2ef8f8d77913",
					"Assiettes plates blanches 27 cm, livrées propres et prêtes à servir."),
			new DemoItem("VERRES-10", "Lot de 10 verres à vin", "Art de la table", 600, 3500, 0, 60, "1620704033114-028772aa85ae",
					"Verres à pied en cristallin, lavage compris."),
			new DemoItem("NAPPE-RONDE", "Nappe ronde blanche", "Art de la table", 500, 3000, 0, 80, "1572319663329-ac47c4efdef0",
					"Nappe en coton 320 cm pour table ronde 10 personnes, repassée."),
			new DemoItem("ARCHE-FLEURIE", "Arche fleurie", "Décoration florale", 18000, 90000, 6000, 2, "1641834919507-b0271fe5186b",
					"Arche en bois habillée de fleurs fraîches de saison et de verdure, pour une cérémonie inoubliable."),
			new DemoItem("CENTRE-TABLE", "Centre de table floral", "Décoration florale", 2500, 6000, 0, 40, "1620704101768-64c46f842956",
					"Composition de roses blanches dans un vase en verre, réalisée la veille de votre événement."),
			new DemoItem("GUIRLANDE-20", "Guirlande lumineuse 20 m", "Lumière et son", 2000, 8000, 1500, 20, "1513625047154-f390b1b0df20",
					"Guirlande guinguette à ampoules chaudes, utilisable en extérieur."),
			new DemoItem("LUSTRE", "Lustre en cristal", "Lumière et son", 6000, 40000, 3000, 4, "1592318072094-2694c98adb73",
					"Lustre à suspendre sous tente ou en salle, effet chic garanti."),
			new DemoItem("ENCEINTE", "Enceinte de sonorisation 1000 W", "Lumière et son", 7000, 60000, 0, 6, "1640533389873-ee8b68984ae4",
					"Enceinte amplifiée avec pied et micro sans fil, pour discours et musique."),
			new DemoItem("PACK-DJ", "Pack DJ (contrôleur et enceintes)", "Lumière et son", 15000, 150000, 5000, 2, "1791983915272-38520e748dc6",
					"Contrôleur DJ, deux enceintes et caisson de basses : tout pour faire danser vos invités."));

	/** Ce que la demonstration cree : supprime au passage en mode reel (DemoPurge). */
	static final List<String> ITEM_REFERENCES = ITEMS.stream().map(DemoItem::reference).toList();
	static final List<String> CATEGORY_NAMES = ITEMS.stream().map(DemoItem::category).distinct().toList();
	static final List<String> PRESTATION_NAMES = List.of("Décoration florale sur mesure", "Mise en place de la salle", "Animation DJ (soirée)",
			"Éclairage d'ambiance");

	private static final Logger log = LoggerFactory.getLogger(DemoService.class);

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final CustomerRepository customers;
	private final CatalogService catalog;
	private final PrestationRepository prestations;
	private final BookingService bookings;
	private final PaymentService payments;
	private final LogisticsService logistics;
	private final ContactMessageRepository messages;
	private final JdbcTemplate jdbc;
	private final EntityManager entityManager;

	public DemoService(UserRepository users, PasswordEncoder passwordEncoder, CustomerRepository customers, CatalogService catalog,
			PrestationRepository prestations, BookingService bookings, PaymentService payments, LogisticsService logistics,
			ContactMessageRepository messages, JdbcTemplate jdbc, EntityManager entityManager) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.customers = customers;
		this.catalog = catalog;
		this.prestations = prestations;
		this.bookings = bookings;
		this.payments = payments;
		this.logistics = logistics;
		this.messages = messages;
		this.jdbc = jdbc;
		this.entityManager = entityManager;
	}

	/** Charge la demonstration si elle n'existe pas encore ; renvoie false si elle etait deja la. */
	@Transactional
	public boolean seed(boolean withPhotos) {
		if (users.existsByEmailIgnoreCase(ACCOUNTS.get(0).email())) return false;

		Map<Role, User> team = new HashMap<>();
		String hash = passwordEncoder.encode(PASSWORD);
		for (DemoAccount a : ACCOUNTS) {
			if (a.role() == Role.CLIENT) team.put(Role.CLIENT, client(a.firstName() + " " + a.lastName(), null, "06 45 12 78 90", a.email(), hash));
			else team.put(a.role(), users.save(new User(a.email(), hash, a.firstName(), a.lastName(), "01 64 00 00 0" + team.size(), a.role())));
		}
		User manager = team.get(Role.GERANT);
		User sales = team.get(Role.COMMERCIAL);
		User driver = team.get(Role.LOGISTIQUE);
		User sophie = team.get(Role.CLIENT);

		Map<String, Long> categoryIds = new HashMap<>();
		Map<String, Long> items = new HashMap<>();
		RestClient http = RestClient.create();
		for (DemoItem d : ITEMS) {
			Long categoryId = categoryIds.computeIfAbsent(d.category(), name -> catalog.createCategory(name).getId());
			Item item = catalog.create(new ItemRequest(d.reference(), d.name(), categoryId, d.description(), d.dayPrice(), d.replacement(),
					d.setup(), d.quantity(), true));
			items.put(d.reference(), item.getId());
			if (withPhotos) addPhoto(http, item, d.photo());
		}
		catalog.putInRepair(items.get("CHAISE-NAP"), new MaintenanceRequest(4, Dates.today(), null, "Pieds à resserrer"), driver);

		Prestation flowers = prestations.save(new Prestation("Décoration florale sur mesure",
				"Notre fleuriste compose vos bouquets, centres de table et arches selon vos couleurs.", 45000, true));
		Prestation service = prestations.save(new Prestation("Mise en place de la salle",
				"Installation des tables, nappes, vaisselle et décoration avant l'arrivée de vos invités.", 30000, true));
		Prestation dj = prestations.save(new Prestation("Animation DJ (soirée)",
				"Un DJ professionnel avec son matériel, de l'apéritif jusqu'à 2 h du matin.", 60000, true));
		prestations.save(new Prestation("Éclairage d'ambiance", "Guirlandes, lustres et projecteurs installés et réglés par notre équipe.",
				15000, true));

		User marc = client("Marc Dubois", null, "06 12 48 30 77", "marc.dubois" + EmailService.DEMO_DOMAIN, null);
		User atelier = client("Nadia Haddad", "Atelier de Brie SARL", "01 60 22 41 18", "nadia.haddad" + EmailService.DEMO_DOMAIN, null);
		User camille = client("Camille Moreau", null, "07 81 26 54 03", "camille.moreau" + EmailService.DEMO_DOMAIN, null);
		User thomas = client("Thomas Leroy", null, "06 70 19 85 42", "thomas.leroy" + EmailService.DEMO_DOMAIN, null);
		User julie = client("Julie Bernard", null, "06 33 57 90 14", "julie.bernard" + EmailService.DEMO_DOMAIN, null);

		LocalDate today = Dates.today();
		// Dossiers termines (dates deplacees dans le passe a la fin) : chiffre d'affaires des derniers mois
		List<long[]> past = new ArrayList<>();
		past.add(new long[] { closed(julie, items, sales, driver, 0, "Anniversaire 40 ans", "TABLE-RONDE", 6, "CHAISE-NAP", 60, "GUIRLANDE-20", 4), 150 });
		past.add(new long[] { closed(atelier, items, sales, driver, 0, "Séminaire annuel", "ENCEINTE", 2, "TABLE-BOIS", 6, "CHAISE-NAP", 50), 115 });
		past.add(new long[] { closed(thomas, items, sales, driver, 2, "Mariage champêtre", "TENTE-6X12", 1, "TABLE-RONDE", 8, "CHAISE-NAP", 80, "NAPPE-RONDE", 8), 80 });
		past.add(new long[] { closed(sophie, items, sales, driver, 0, "Baptême", "BARNUM-3X6", 1, "TABLE-BOIS", 3, "CENTRE-TABLE", 3), 45 });
		past.add(new long[] { closed(camille, items, sales, driver, 1, "Fête de fin d'études", "PACK-DJ", 1, "GUIRLANDE-20", 6, "BARNUM-3X6", 2), 18 });

		// En cours : livree hier soir, l'evenement a lieu demain
		Booking ongoing = request(marc, items, today.plusDays(1), today.plusDays(2), DeliveryMode.LIVRAISON, "Garden party",
				List.of(), "BARNUM-3X6", 2, "TABLE-RONDE", 4, "CHAISE-NAP", 40);
		quote(ongoing, sales, 0);
		pay(ongoing, PaymentMethod.VIREMENT, ongoing.getDepositCents(), sales);
		logistics.deliver(bookings.get(ongoing.getId()), PaymentMethod.CHEQUE, driver);

		// Confirmees : planning des prochaines semaines
		Booking wedding = request(sophie, items, today.plusDays(24), today.plusDays(25), DeliveryMode.LIVRAISON, "Mariage, 90 invités",
				List.of(flowers.getId(), service.getId()), "TENTE-6X12", 1, "TABLE-RONDE", 9, "CHAISE-NAP", 90, "NAPPE-RONDE", 9,
				"ARCHE-FLEURIE", 1, "CENTRE-TABLE", 9);
		quote(wedding, sales, 20000);
		pay(wedding, PaymentMethod.VIREMENT, wedding.getDepositCents(), sales);
		Booking seminar = request(atelier, items, today.plusDays(9), today.plusDays(9), DeliveryMode.RETRAIT, "Séminaire commercial",
				List.of(), "ENCEINTE", 2, "TABLE-BOIS", 5);
		quote(seminar, sales, 0);
		pay(seminar, PaymentMethod.CHEQUE, seminar.getDepositCents(), sales);

		// Devis envoyes, en attente du client (Sophie peut payer son acompte en ligne)
		Booking birthday = request(sophie, items, today.plusDays(40), today.plusDays(40), DeliveryMode.RETRAIT, "Anniversaire de ma fille",
				List.of(), "BARNUM-3X6", 1, "GUIRLANDE-20", 3, "TABLE-BOIS", 2);
		quote(birthday, sales, 0);
		Booking party = request(camille, items, today.plusDays(33), today.plusDays(34), DeliveryMode.LIVRAISON, "Soirée d'été",
				List.of(dj.getId()), "PACK-DJ", 1, "GUIRLANDE-20", 5, "LUSTRE", 2);
		quote(party, sales, 5000);

		// Nouvelles demandes a traiter
		request(thomas, items, today.plusDays(52), today.plusDays(53), DeliveryMode.LIVRAISON, "Fiançailles, une soixantaine d'invités",
				List.of(flowers.getId()), "TENTE-6X12", 1, "CHAISE-NAP", 60, "TABLE-RONDE", 6, "CENTRE-TABLE", 6);
		request(julie, items, today.plusDays(19), today.plusDays(19), DeliveryMode.RETRAIT, "Communion", List.of(), "TABLE-RONDE", 3,
				"CHAISE-NAP", 30, "ASSIETTES-10", 3, "VERRES-10", 3);

		messages.save(new ContactMessage("Élodie Garnier", "elodie.garnier" + EmailService.DEMO_DOMAIN, "06 21 43 65 87", today.plusDays(75),
				"Bonjour, nous organisons un mariage de 120 personnes près de Fontainebleau. Proposez-vous aussi le traiteur ?"));
		messages.save(new ContactMessage("Mairie de Vaux-le-Pénil (exemple)", "fetes" + EmailService.DEMO_DOMAIN, null, today.plusDays(60),
				"Pourriez-vous nous faire un devis pour 4 barnums et une sonorisation pour la fête du village ?"));

		// Les dossiers termines passent dans le passe : dates, historique, paiements et factures
		entityManager.flush();
		for (long[] p : past) shiftToPast(p[0], (int) p[1]);
		entityManager.clear();
		log.info("Démonstration FloreEvent chargée");
		return true;
	}

	private User client(String fullName, String company, String phone, String email, String hash) {
		String[] names = fullName.split(" ", 2);
		User user = new User(email, hash != null ? hash : passwordEncoder.encode(UUID.randomUUID().toString()), names[0], names[1], phone,
				Role.CLIENT);
		user.acceptTerms(Terms.VERSION);
		user.setCustomer(customers.save(new Customer(fullName, company, phone, email)));
		return users.save(user);
	}

	/** Demande du client : dates, mode, note, prestations, puis (reference, quantite). */
	private Booking request(User client, Map<String, Long> items, LocalDate start, LocalDate end, DeliveryMode mode, String note,
			List<Long> prestationIds, Object... lines) {
		List<LineRequest> chosen = new ArrayList<>();
		for (int i = 0; i < lines.length; i += 2) chosen.add(new LineRequest(items.get((String) lines[i]), (Integer) lines[i + 1]));
		boolean delivery = mode == DeliveryMode.LIVRAISON;
		try {
			return bookings.request(new BookingRequest(start, end, mode, delivery ? "12 rue Saint-Barthélemy 77000 Melun" : null, delivery, note,
					chosen, prestationIds), client);
		} catch (RuntimeException e) {
			// Service d'adresses injoignable : la demande se fait en retrait
			log.warn("Démonstration : livraison impossible ({}), demande en retrait", e.getMessage());
			return bookings.request(new BookingRequest(start, end, DeliveryMode.RETRAIT, null, false, note, chosen, prestationIds), client);
		}
	}

	private void quote(Booking booking, User author, int discount) {
		bookings.sendQuote(booking.getId(), new QuoteRequest(discount, discount > 0 ? "Remise offerte pour votre fidélité." : null), author);
	}

	private void pay(Booking booking, PaymentMethod method, int amount, User author) {
		payments.recordOffline(bookings.get(booking.getId()), new OfflinePaymentRequest(method, amount, null), author);
	}

	/** Dossier complet : demande, devis, acompte, livraison, retour (avec casse eventuelle), solde, cloture. */
	private long closed(User client, Map<String, Long> items, User sales, User driver, int broken, String note, Object... lines) {
		Booking b = request(client, items, Dates.today().plusDays(1), Dates.today().plusDays(1), DeliveryMode.RETRAIT, note, List.of(), lines);
		quote(b, sales, 0);
		pay(b, PaymentMethod.VIREMENT, b.getDepositCents(), sales);
		Booking booking = bookings.get(b.getId());
		logistics.deliver(booking, PaymentMethod.CHEQUE, driver);
		List<ReturnLine> counts = new ArrayList<>();
		for (BookingLine line : booking.getLines()) {
			int damaged = counts.isEmpty() ? 0 : Math.min(broken, line.getQuantity());
			if (damaged > 0) broken = 0;
			counts.add(new ReturnLine(line.getItem().getId(), line.getQuantity() - damaged, damaged, 0));
		}
		logistics.recordReturn(booking, new ReturnRequest(counts, null), driver);
		if (booking.getBalanceCents() > 0)
			payments.recordOffline(booking, new OfflinePaymentRequest(PaymentMethod.ESPECES, booking.getBalanceCents(), null), driver);
		logistics.close(booking, sales);
		return booking.getId();
	}

	/** L'evenement a eu lieu il y a « days » jours : dates et horodatages coherents. */
	private void shiftToPast(long bookingId, int days) {
		LocalDate start = Dates.today().minusDays(days);
		Timestamp before = Timestamp.from(Instant.now().minus(Duration.ofDays(days + 12)));
		Timestamp after = Timestamp.from(Instant.now().minus(Duration.ofDays(days - 1)));
		jdbc.update("update booking set start_date = ?, end_date = ?, created_at = ?, updated_at = ?, delivered_at = ?, returned_at = ? where id = ?",
				start, start, before, after, Timestamp.from(Instant.now().minus(Duration.ofDays(days + 1))), after, bookingId);
		jdbc.update("update booking_event set created_at = ? where booking_id = ?", after, bookingId);
		jdbc.update("update payment set created_at = ?, paid_at = ? where booking_id = ?", after, after, bookingId);
		jdbc.update("update invoice set issued_at = ? where booking_id = ?", after, bookingId);
	}

	/** Photo libre de droits (Unsplash) telechargee au format JPEG ; sans connexion, l'article reste sans photo. */
	private void addPhoto(RestClient http, Item item, String photoId) {
		try {
			byte[] bytes = http.get().uri("https://images.unsplash.com/photo-" + photoId + "?w=600&h=450&fit=crop&q=70&fm=jpg").retrieve()
					.body(byte[].class);
			if (bytes != null) catalog.addImage(item.getId(), "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes));
		} catch (RuntimeException e) {
			log.warn("Photo de démonstration indisponible pour {} : {}", item.getName(), e.getMessage());
		}
	}
}
