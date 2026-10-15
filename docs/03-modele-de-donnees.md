# Modèle de données

Montants en **centimes d'euro** (R5). Les tables sont créées par des migrations Flyway versionnées.

```mermaid
erDiagram
    APP_USER ||--o| CUSTOMER : "compte client"
    CATEGORY ||--o{ ITEM : contient
    ITEM ||--o{ ITEM_IMAGE : photos
    ITEM ||--o{ MAINTENANCE : "en réparation"
    CUSTOMER ||--o{ BOOKING : réserve
    BOOKING ||--|{ BOOKING_LINE : "articles loués"
    BOOKING ||--o{ BOOKING_SERVICE : prestations
    BOOKING ||--o{ BOOKING_EVENT : historique
    BOOKING ||--o{ PAYMENT : paiements
    BOOKING ||--o{ DELIVERY : "livraison et retour"
    BOOKING ||--o| RETURN_CHECK : "état des lieux"
    BOOKING ||--o{ INVOICE : factures
    ITEM ||--o{ BOOKING_LINE : "loué dans"
    SERVICE ||--o{ BOOKING_SERVICE : "ajouté à"
    APP_USER ||--o{ DELIVERY : "assignée à"

    APP_USER {
        bigint id PK
        string email UK
        string password_hash
        string role "GERANT, COMMERCIAL, LOGISTIQUE, CLIENT"
        boolean active
    }
    CUSTOMER {
        bigint id PK
        string full_name
        string company "facultatif"
        string phone
        string email
    }
    ITEM {
        bigint id PK
        string reference UK
        string name
        bigint category_id FK
        int day_price_cents
        int replacement_value_cents
        int setup_price_cents "installation"
        int quantity "stock total"
        boolean active
    }
    MAINTENANCE {
        bigint id PK
        bigint item_id FK
        int quantity
        date from_date
        date to_date "vide : jusqu'à nouvel ordre"
    }
    BOOKING {
        bigint id PK
        string reference UK "RES-2026-00001"
        bigint customer_id FK
        string status "DEMANDE, DEVIS, CONFIRMEE, EN_COURS, RETOURNEE, CLOTUREE, ANNULEE"
        date start_date "début de l'événement"
        date end_date
        string delivery_mode "RETRAIT, LIVRAISON"
        string address
        int distance_km
        int total_cents "TTC, prix figés (R3)"
        int deposit_cents "acompte"
        int security_deposit_cents "caution"
    }
    BOOKING_LINE {
        bigint id PK
        bigint booking_id FK
        bigint item_id FK
        int quantity
        int day_price_cents "figé"
        int days
    }
    PAYMENT {
        bigint id PK
        bigint booking_id FK
        string kind "ACOMPTE, SOLDE, CASSE, REMBOURSEMENT"
        string method "CARTE, VIREMENT, CHEQUE, ESPECES"
        int amount_cents
        string stripe_session_id
    }
    DELIVERY {
        bigint id PK
        bigint booking_id FK
        string kind "LIVRAISON, RETOUR"
        date planned_date
        string slot "matin, après-midi"
        bigint driver_id FK
        string status
    }
    INVOICE {
        bigint id PK
        string number UK "FAC-2026-00001, sans trou (R6)"
        bigint booking_id FK
        int amount_cents
    }
```

## Calcul de la disponibilité (R1)

Pour un article et une période `[début, fin]`, la période **bloquée** d'une réservation va de la veille de son début au
lendemain de sa fin (préparation et nettoyage). Disponible =

```
stock de l'article
 − quantités réservées par les réservations CONFIRMEE ou EN_COURS dont la période bloquée chevauche [début, fin]
 − quantités en réparation sur [début, fin]
```

Deux périodes `[a, b]` et `[c, d]` se chevauchent quand `a ≤ d` et `c ≤ b`. Au moment de confirmer une réservation, les articles
concernés sont **verrouillés** (`SELECT … FOR UPDATE`) puis la disponibilité est recalculée (R2).
