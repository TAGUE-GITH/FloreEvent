# Mise en ligne de FloreEvent

Tout est gratuit : **Neon** (base PostgreSQL), **Render** (API Spring Boot), **Vercel** (site React), **Brevo** (emails) et,
en option, **Stripe en mode test** (paiement de l'acompte). Aucun mot de passe ni aucune clé n'est écrit dans le code : on les
saisit uniquement dans les tableaux de bord des hébergeurs.

```
Navigateur ──► Vercel (site React) ──► Render (API Spring Boot, Docker) ──► Neon (PostgreSQL)
                                                 ├──► Brevo (emails : devis, confirmation, rappel, mot de passe oublié)
                                                 └──► Stripe (acompte par carte, mode test, webhook signé)
```

Cette mise en ligne est une **démonstration** (`APP_DEMO=true`) : matériel, clients, réservations et chiffres sont fictifs.
L'espace équipe (`/espace-equipe`) propose un compte par rôle (gérant, commercial, logistique), la connexion client
(`/connexion`) propose le compte client. Les vraies données de FloreEvent iront dans une installation séparée (`APP_DEMO=false`).

## 1. Base de données — Neon

1. Sur [console.neon.tech](https://console.neon.tech), créer un projet `floreevent` (région **Europe, Frankfurt**).
2. **Connect** → décocher **Connection pooling** → affichage **Parameters only** : `PGHOST`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.

| Variable (Render) | Valeur |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://PGHOST/neondb?sslmode=require` (remplacer `PGHOST`) |
| `DATABASE_USERNAME` | `PGUSER` |
| `DATABASE_PASSWORD` | `PGPASSWORD` |

Les tables sont créées automatiquement par Flyway au premier démarrage.

## 2. API — Render

1. [dashboard.render.com](https://dashboard.render.com) → **New** → **Blueprint** → dépôt `FloreEvent`. Render lit
   [`render.yaml`](render.yaml) et prépare le service `floreevent-api`.
2. Saisir les valeurs : les trois de Neon, `FRONTEND_URL` (adresse Vercel de l'étape 3, sans `/` final), `BREVO_API_KEY` et
   `MAIL_SENDER_EMAIL` (étape 4). Laisser `STRIPE_SECRET_KEY` et `STRIPE_WEBHOOK_SECRET` vides pour commencer.
3. **Apply** (5 à 10 minutes la première fois). Vérification : `https://floreevent-api.onrender.com/api/health` → `{"status":"ok"}`.

Au premier démarrage, la démonstration est chargée : 14 articles avec photos, 4 prestations, 12 réservations à toutes les
étapes, et 4 comptes (mot de passe `demo-floreevent-2026`).

> Offre gratuite : le service s'endort après 15 minutes sans visite et met environ une minute à se réveiller.

## 3. Site — Vercel

1. [vercel.com/new](https://vercel.com/new) → importer `FloreEvent`, **Root Directory** : `frontend`.
2. **Environment Variables** : `VITE_API_URL` = `https://floreevent-api.onrender.com` (sans `/` final).
3. **Deploy**, puis reporter l'adresse du site dans `FRONTEND_URL` sur Render.

## 4. Emails — Brevo

1. [app.brevo.com](https://app.brevo.com) → **Senders** : vérifier l'adresse d'expédition (`MAIL_SENDER_EMAIL`).
2. **SMTP & API** → **API Keys** → nouvelle clé (`BREVO_API_KEY`).

Sans clé, les emails sont seulement écrits dans le journal du serveur. Les adresses de démonstration ne reçoivent jamais d'email.

## 5. Paiement — Stripe en mode test (facultatif)

Sans clé Stripe, le site utilise une page de paiement **simulée**. Pour la vraie page Stripe (mode test, carte `4242 4242 4242 4242`) :

1. [dashboard.stripe.com](https://dashboard.stripe.com) → rester en **mode test** → **Développeurs** → **Clés API** :
   la clé secrète `sk_test_…` va dans `STRIPE_SECRET_KEY`.
2. **Développeurs** → **Webhooks** → **Ajouter un endpoint** : `https://floreevent-api.onrender.com/api/stripe/webhook`,
   événement `checkout.session.completed`. Le secret de signature `whsec_…` va dans `STRIPE_WEBHOOK_SECRET`.
