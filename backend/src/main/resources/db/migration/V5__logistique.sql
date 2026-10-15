-- Sprint 4 : livraison (caution recue), retour et etat des lieux, casse retenue sur la caution (R7)

ALTER TABLE booking ADD COLUMN delivered_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE booking ADD COLUMN returned_at TIMESTAMP WITH TIME ZONE;
-- Caution remise a la livraison (cheque, especes ou virement), restituee a la cloture
ALTER TABLE booking ADD COLUMN security_deposit_method VARCHAR(20);
ALTER TABLE booking ADD COLUMN security_deposit_returned_cents INTEGER CHECK (security_deposit_returned_cents >= 0);
-- Casse et manquants factures au retour (valeur de remplacement figee, R3)
ALTER TABLE booking ADD COLUMN damage_cents INTEGER NOT NULL DEFAULT 0 CHECK (damage_cents >= 0);

-- Etat des lieux : quantites rendues en bon etat, abimees, manquantes
ALTER TABLE booking_line ADD COLUMN returned_quantity INTEGER CHECK (returned_quantity >= 0);
ALTER TABLE booking_line ADD COLUMN damaged_quantity INTEGER CHECK (damaged_quantity >= 0);
ALTER TABLE booking_line ADD COLUMN missing_quantity INTEGER CHECK (missing_quantity >= 0);
