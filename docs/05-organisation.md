# Organisation du projet

## Méthode

- **Sprints courts**, chacun livrant des fonctionnalités utilisables de bout en bout (serveur, site, tests).
- **Tests d'abord (TDD)** : les critères d'acceptation des récits deviennent des tests avant le code.
- **Vérification à chaque étape** : tests automatiques, essai dans un vrai navigateur (téléphone et ordinateur),
  intégration continue verte avant de passer au sprint suivant.

## Planning

| Sprint | Contenu |
|---|---|
| 0 | Analyse des besoins, conception, environnement, intégration continue |
| 1 | Comptes et rôles, catalogue, stock et matériel en réparation |
| 2 | Disponibilités par dates, catalogue public, compte client, demande de devis |
| 3 | Réservations, acompte par carte (Stripe test), devis et factures PDF, annulation |
| 4 | Planning logistique, livraisons, retours et état des lieux, caution et casse |
| 5 | Services, emails, tableau de bord |
| 6 | Site vitrine, mot de passe oublié, démonstration, mise en ligne |

## Outils

Git et GitHub (historique, intégration continue), Maven, npm, Docker, PostgreSQL, Render, Neon, Vercel, Brevo, Stripe.

## Confidentialité

FloreEvent est une vraie entreprise : ses données (clients, chiffre d'affaires) ne vont jamais dans le dépôt ni dans la
démonstration publique, qui utilise uniquement des données fictives. Les mots de passe et clés (base de données, Stripe, Brevo)
sont saisis chez les hébergeurs, jamais dans le code.
