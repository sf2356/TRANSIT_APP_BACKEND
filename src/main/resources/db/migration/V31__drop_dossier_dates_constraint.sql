-- Retrait de la contrainte (demande utilisateur) : la date d'échéance peut désormais être
-- antérieure à la date d'ouverture du dossier, si besoin métier (ex. échéance déjà connue
-- avant l'ouverture formelle du dossier dans le système).
ALTER TABLE dossiers DROP CONSTRAINT chk_dossier_dates;