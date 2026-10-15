# Architecture

```
Navigateur (React 19 + TypeScript, Vite, Tailwind)  ──►  API REST Spring Boot 4 (Java 21)  ──►  PostgreSQL (Neon)
        hébergé sur Vercel                                 hébergée sur Render (Docker)
                                                              ├──► Stripe (paiement de l'acompte, mode test, webhook signé)
                                                              ├──► API Adresse de l'État (adresse et distance depuis Melun)
                                                              └──► Brevo (emails)
```

## Serveur (backend/)

- **Spring Boot 4.1** : Web MVC, Validation, Data JPA (Hibernate), Security, Flyway.
- **Organisation par domaine métier** : `auth`, `user`, `catalog`, `availability`, `booking`, `payment`, `logistics`, `document`,
  `dashboard`, `common`. Chaque domaine a ses entités, ses dépôts, ses services (règles métier, transactions) et ses contrôleurs.
- **Sécurité** : JWT sans session, BCrypt, droits par rôle avec `@PreAuthorize` sur chaque action.
- **Concurrence** : verrou pessimiste sur les articles au moment de confirmer une réservation (R2).
- **Erreurs** : toutes au format `{ "message": "...", "errors": { "champ": "..." } }`, en français.
- **PDF** : OpenPDF (devis, factures, bons de livraison).
- **Tests** : JUnit 5 et MockMvc (API complète sur base H2 en mode PostgreSQL), Mockito (services), et en intégration continue
  sur une vraie base PostgreSQL.

## Site (frontend/)

- **React 19**, **TypeScript**, **Vite**, **Tailwind CSS 4**, **React Router**, **TanStack Query** (cache des données),
  **axios**, icônes **lucide**.
- Site public (catalogue, disponibilités, devis), espace client, espace équipe (planning, réservations, catalogue).
- **Tests** : Vitest et Testing Library ; essais de bout en bout dans un vrai navigateur (téléphone et ordinateur).

## Paiement de l'acompte (Stripe, mode test)

1. Le client accepte son devis : l'API crée une **session Stripe Checkout** et renvoie son adresse.
2. Le client paie sur la page Stripe (le site ne voit jamais la carte).
3. Stripe appelle le **webhook** de l'API ; la signature est vérifiée, la disponibilité recalculée sous verrou, puis la réservation
   passe à `CONFIRMEE`. Si le matériel n'est plus disponible, le paiement est remboursé automatiquement.

## Intégration continue

GitHub Actions à chaque envoi : tests du serveur sur PostgreSQL 17, puis lint, tests et construction du site.
