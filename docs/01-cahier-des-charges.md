# Cahier des charges — FloreEvent

## 1. Contexte

FloreEvent, entreprise de location événementielle basée à **Melun (Seine-et-Marne)**, loue du matériel (tentes et barnums,
tables, chaises, linge de table, vaisselle, sonorisation, éclairage, décoration) et propose des services (livraison,
installation, décoration florale) pour les mariages, anniversaires, séminaires et fêtes.

Aujourd'hui, les demandes arrivent par téléphone et par email, les disponibilités se vérifient à la main et les devis se
font sur tableur : risque de **louer deux fois le même matériel aux mêmes dates**, devis lents à produire, planning des
livraisons difficile à suivre, casse non facturée.

**Objectif :** digitaliser toute la chaîne, de la demande du client au retour du matériel.

## 2. Hypothèses de départ (à confirmer avec FloreEvent)

Les réponses de l'entreprise remplaceront ces valeurs ; toutes sont des paramètres modifiables, pas du code en dur.

| Sujet | Hypothèse retenue |
|---|---|
| Prix | Tarif par article pour **1 jour** ; chaque jour supplémentaire coûte **50 %** du tarif journalier |
| Prix affichés | **TTC**, TVA **20 %** (factures avec HT, TVA et TTC) |
| Livraison | Par **zones** autour de Melun selon la distance : ≤ 15 km **40 €**, ≤ 30 km **70 €**, ≤ 50 km **110 €**, au-delà sur devis. Retrait en entrepôt gratuit |
| Installation | Forfait par article quand c'est utile (ex. barnum), sinon gratuite |
| Acompte | **30 %** à la réservation, payé en ligne par carte (Stripe, **mode test**) ; solde au plus tard à la livraison |
| Caution | **30 %** de la valeur du matériel loué (minimum 150 €), déposée à la livraison (chèque ou empreinte), rendue après le retour |
| Annulation | Plus de 30 jours avant : acompte remboursé ; de 30 à 8 jours : acompte conservé ; moins de 8 jours : 50 % du total dû |
| Casse / perte | Facturée à la **valeur de remplacement** de l'article (renseignée dans le catalogue), retenue sur la caution |
| Préparation | Un article loué est indisponible **la veille** (préparation) et **le lendemain** (nettoyage, contrôle) de la location |
| Devis | Valable **7 jours** ; il ne bloque le matériel qu'une fois l'acompte payé |
| Équipe | Gérant, commerciaux, équipe logistique (livraison, installation, retours) |
| Coordonnées, horaires, SIRET | À fournir par l'entreprise (affichés sur le site et les factures) |

## 3. Acteurs et rôles

| Rôle | Droits principaux |
|---|---|
| **Visiteur** | Consulter le catalogue, vérifier les disponibilités pour ses dates, créer un compte |
| **Client** | Composer et envoyer une demande de devis, payer l'acompte, suivre ses réservations, télécharger devis et factures |
| **Commercial** | Gérer les clients, devis et réservations, enregistrer les paiements, consulter le planning |
| **Logistique** | Voir le planning du jour, ses livraisons et retours, faire l'état des lieux du matériel rendu |
| **Gérant** | Tout, plus le catalogue, les tarifs, l'équipe et le tableau de bord |

## 4. Règles métier

- **R1 — Disponibilité :** pour chaque article et chaque période, la quantité réservée (réservations confirmées ou en cours,
  préparation comprise) plus la quantité en réparation ne dépasse jamais le stock.
- **R2 — Pas de double réservation :** la vérification et l'enregistrement se font sous verrou ; deux clients qui paient en même
  temps le dernier exemplaire ne peuvent pas l'obtenir tous les deux.
- **R3 — Prix figés :** les prix d'une réservation sont copiés au moment du devis ; un changement de tarif ne la modifie pas.
- **R4 — Cycle de vie :** `DEMANDE` → `DEVIS` → `CONFIRMEE` (acompte payé) → `EN_COURS` (livrée) → `RETOURNEE` → `CLOTUREE`,
  ou `ANNULEE`. Chaque étape est tracée (qui, quand).
- **R5 — Montants :** stockés en **centimes d'euro** (entiers), jamais en nombres à virgule.
- **R6 — Numérotation :** références lisibles (`RES-2026-00001`) ; les **factures** ont une numérotation continue, sans trou,
  comme l'exige la loi.
- **R7 — Caution et casse :** l'état des lieux au retour enregistre les articles abîmés ou manquants ; leur valeur de remplacement
  est facturée et retenue sur la caution.
- **R8 — Paiement :** le site ne voit jamais les numéros de carte (page de paiement Stripe) ; seule la confirmation signée de Stripe
  (webhook) valide un paiement.
- **R9 — Données personnelles :** RGPD (consentement aux conditions, droits d'accès et de suppression, durées de conservation).

## 5. Besoins non fonctionnels

- **Responsive** : téléphone (équipe logistique sur le terrain), tablette, ordinateur.
- **Sécurité** : mots de passe chiffrés (BCrypt), jetons JWT, droits vérifiés par le serveur sur chaque action.
- **Qualité** : tests automatiques (JUnit, Mockito, Vitest), vérification automatique à chaque envoi (GitHub Actions, PostgreSQL).
- **Hébergement gratuit** : Render (API), Neon (PostgreSQL), Vercel (site), Brevo (emails), Stripe (paiements en mode test).
- **Démonstration publique** avec des données fictives ; les vraies données de FloreEvent restent privées.

## 6. Hors périmètre (pour l'instant)

Application mobile native, comptabilité complète, gestion des fournisseurs et des achats, signature électronique des contrats.
