-- Compte d'essai (demande utilisateur) : toute nouvelle inscription via /register obtient
-- automatiquement 14 jours d'accès complet. NULL = pas (ou plus) un compte d'essai — ainsi
-- convertir un essai en compte payant se fait simplement en repassant cette colonne à NULL,
-- sans avoir besoin d'un statut séparé à synchroniser.
ALTER TABLE entreprises ADD COLUMN date_expiration_essai DATE;