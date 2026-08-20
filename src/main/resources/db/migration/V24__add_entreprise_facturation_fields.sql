-- Champs nécessaires à une facture PDF professionnelle et légalement complète
-- (RCCM/IFU obligatoires en zone UEMOA, coordonnées bancaires pour les virements, cachet).
ALTER TABLE entreprises ADD COLUMN rccm VARCHAR(100);
ALTER TABLE entreprises ADD COLUMN ifu VARCHAR(100);
ALTER TABLE entreprises ADD COLUMN site_web VARCHAR(255);
ALTER TABLE entreprises ADD COLUMN banque VARCHAR(255);
ALTER TABLE entreprises ADD COLUMN iban VARCHAR(100);
ALTER TABLE entreprises ADD COLUMN cachet VARCHAR(500);
ALTER TABLE entreprises ADD COLUMN mentions_legales TEXT;