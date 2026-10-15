# FloreEvent — location événementielle à Melun

Application web de **location de matériel et de services événementiels** (tentes, mobilier, vaisselle, sonorisation,
décoration florale…) pour FloreEvent, à Melun (Seine-et-Marne) : catalogue en ligne, disponibilités par dates, devis calculés
automatiquement, acompte par carte, planning des livraisons, état des lieux au retour.

> **État du projet :** Sprints 0 à 6 terminés : catalogue et disponibilités, devis en ligne, acompte Stripe (test), factures PDF,
> logistique (planning, livraisons, retours, caution), prestations, emails, tableau de bord, site vitrine et démonstration.
> Mise en ligne : voir [DEPLOIEMENT.md](DEPLOIEMENT.md).

**Démonstration en ligne** (données fictives) : [flore-event.vercel.app](https://flore-event.vercel.app)
— espace équipe : [/espace-equipe](https://flore-event.vercel.app/espace-equipe) (gérant, commercial, logistique),
espace client : [/connexion](https://flore-event.vercel.app/connexion). Les comptes d'essai sont proposés sur chaque page de connexion.

## Le besoin

Les demandes arrivent par téléphone et par email, les disponibilités se vérifient à la main et les devis se font sur tableur :
risque de louer deux fois le même matériel aux mêmes dates, devis lents, planning des livraisons difficile à suivre, casse non
facturée. FloreEvent **digitalise toute la chaîne**, de la demande du client au retour du matériel.

## Conception (Sprint 0)

| Document | Contenu |
|---|---|
| [Cahier des charges](docs/01-cahier-des-charges.md) | Contexte, hypothèses à confirmer, rôles, règles métier (disponibilité, prix figés, caution…) |
| [Récits utilisateurs](docs/02-user-stories.md) | 24 récits répartis en 6 sprints, critères d'acceptation, définition de « terminé » |
| [Modèle de données](docs/03-modele-de-donnees.md) | Schéma des tables et calcul de la disponibilité par période |
| [Architecture](docs/04-architecture.md) | Spring Boot, React, PostgreSQL, Stripe, API Adresse, Brevo |
| [Organisation](docs/05-organisation.md) | Méthode, planning des sprints, confidentialité des données |

## Technologies

- **Serveur :** Java 21, Spring Boot 4 (Web, Validation, Data JPA, Security, Flyway), PostgreSQL, JUnit 5, Mockito
- **Site :** React 19, TypeScript, Vite, Tailwind CSS 4, React Router, TanStack Query, Vitest, Testing Library
- **Services :** Stripe (paiement de l'acompte, mode test), API Adresse de l'État (distance de livraison), Brevo (emails)
- **Outils :** Docker, GitHub Actions (tests sur PostgreSQL à chaque envoi), Render, Neon, Vercel

## Planning

| Sprint | Contenu | État |
|---|---|---|
| 0 | Analyse des besoins, conception, environnement, intégration continue | ✅ |
| 1 | Comptes et rôles, catalogue, stock et matériel en réparation | ✅ |
| 2 | Disponibilités par dates, catalogue public, compte client, demande de devis | ✅ |
| 3 | Réservations, acompte par carte (Stripe test), devis et factures PDF, annulation | ✅ |
| 4 | Planning logistique, livraisons, retours et état des lieux, caution et casse | ✅ |
| 5 | Services, emails, tableau de bord | ✅ |
| 6 | Site vitrine, mot de passe oublié, démonstration, mise en ligne | ✅ |

## Lancer le projet en local

```bash
cd backend && ./mvnw spring-boot:run      # API sur http://localhost:8080 (base H2 en mémoire ; APP_DEMO=true pour la démo)
cd frontend && npm install && npm run dev # site sur http://localhost:5173
```

Avec Docker (API + PostgreSQL) : `docker compose up --build`.

### Paiement de l'acompte (Stripe, mode test)

Sans clé Stripe, le site local utilise une **page de paiement simulée** (aucune carte, aucun argent échangé). Pour le vrai
paiement en mode test, les clés sont saisies **chez l'hébergeur**, jamais dans le code :

| Variable | Valeur |
|---|---|
| `STRIPE_SECRET_KEY` | clé secrète de test (`sk_test_…`), tableau de bord Stripe → Développeurs → Clés API |
| `STRIPE_WEBHOOK_SECRET` | secret du webhook (`whsec_…`) pointant vers `https://<api>/api/stripe/webhook`, événement `checkout.session.completed` |

La réservation n'est confirmée que par ce webhook signé (R8) ; la disponibilité est revérifiée sous verrou, et le client est
remboursé automatiquement si le matériel a été réservé entre-temps (R2).

## Tests

```bash
cd backend && ./mvnw test    # 76 tests : JUnit 5, MockMvc
cd frontend && npm test      # 39 tests : Vitest, Testing Library
```

Les tests sont écrits avant le code (TDD) à partir des critères d'acceptation. GitHub Actions les exécute à chaque envoi, avec une
vraie base PostgreSQL.

## Confidentialité

FloreEvent est une vraie entreprise : ses données ne sont jamais dans ce dépôt. La démonstration publique utilisera uniquement
des données fictives, et les clés (base de données, Stripe, Brevo) sont saisies chez les hébergeurs.
