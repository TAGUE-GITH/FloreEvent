# Récits utilisateurs et sprints

Format : *En tant que … je veux … afin de …*, avec des critères d'acceptation qui deviennent des tests automatiques.

## Sprint 1 — Comptes, catalogue et stock

- **US-01 Connexion** — En tant qu'employé, je me connecte avec mon email et mon mot de passe afin d'accéder à mon espace.
  *Critères :* message identique si l'email ou le mot de passe est faux ; compte désactivé refusé.
- **US-02 Équipe** — En tant que gérant, je crée les comptes de l'équipe avec un rôle (commercial, logistique, gérant).
- **US-03 Catalogue** — En tant que gérant, je gère catégories et articles : nom, description, photos, prix par jour,
  valeur de remplacement, quantité en stock, forfait d'installation, article actif ou non.
- **US-04 Matériel en réparation** — En tant que gérant, je mets des exemplaires en réparation pour une période :
  ils ne sont plus proposés à la location (R1).

## Sprint 2 — Disponibilités, catalogue public et demande de devis

- **US-05 Disponibilité** — En tant que visiteur, je choisis les dates de mon événement et je vois les quantités disponibles
  de chaque article (R1, préparation comprise).
- **US-06 Compte client** — En tant que client, je crée mon compte en acceptant les conditions générales (RGPD).
- **US-07 Demande de devis** — En tant que client, je compose ma sélection (articles, quantités, dates, livraison ou retrait,
  adresse) et je vois le prix calculé : location, jours supplémentaires, livraison selon la zone, installation, TVA, acompte, caution.
  *Critères :* refus si une quantité dépasse la disponibilité ; adresse vérifiée (API Adresse de l'État).
- **US-08 Espace client** — Je retrouve mes demandes, devis et réservations avec leur état.

## Sprint 3 — Réservations, paiement et documents

- **US-09 Traitement des demandes** — En tant que commercial, je transforme une demande en devis (ajustements, remise, message)
  et je l'envoie au client.
- **US-10 Paiement de l'acompte** — En tant que client, j'accepte le devis et je paie l'acompte par carte (Stripe, mode test).
  *Critères :* disponibilité revérifiée sous verrou au moment du paiement (R2) ; réservation confirmée seulement par le webhook signé (R8).
- **US-11 Documents** — Devis, confirmation et facture en PDF (numérotation continue des factures, R6).
- **US-12 Paiements hors ligne** — En tant que commercial, j'enregistre un solde payé par virement, chèque ou espèces.
- **US-13 Annulation** — Selon la date, remboursement ou retenue de l'acompte (règle du cahier des charges).

## Sprint 4 — Logistique : planning, livraisons et retours

- **US-14 Planning** — En tant que logistique, je vois les livraisons, installations et retours du jour et de la semaine,
  avec l'adresse et le matériel à charger.
- **US-15 Bon de livraison** — PDF avec la liste du matériel et l'emplacement de signature du client.
- **US-16 Livraison** — Je marque la réservation comme livrée (EN_COURS) et j'enregistre la caution.
- **US-17 État des lieux au retour** — J'indique pour chaque article la quantité rendue, abîmée ou manquante ;
  la casse est facturée et retenue sur la caution (R7), le matériel abîmé passe en réparation.

## Sprint 5 — Services, emails et tableau de bord

- **US-18 Services** — Décoration florale et autres prestations sur devis, ajoutées à une réservation.
- **US-19 Emails** — Confirmation de réservation, rappel avant l'événement, devis envoyé (Brevo).
- **US-20 Tableau de bord** — Chiffre d'affaires, réservations à venir, taux d'occupation du matériel, articles les plus loués,
  cautions en attente.

## Sprint 6 — Site vitrine, démonstration et mise en ligne

- **US-21 Site vitrine** — Accueil avec photos, galerie de réalisations, contact, horaires, mentions légales, CGV, confidentialité.
- **US-22 Mot de passe oublié** — Lien à usage unique par email.
- **US-23 Démonstration** — Données fictives et un compte par rôle.
- **US-24 Mise en ligne** — Render, Neon, Vercel, Brevo, Stripe (mode test).

## Définition de « terminé »

Un récit est terminé quand ses critères sont couverts par des tests qui passent, que la vérification automatique est verte,
que l'écran fonctionne sur téléphone et sur ordinateur (essai dans un vrai navigateur) et que le code est sur GitHub.
