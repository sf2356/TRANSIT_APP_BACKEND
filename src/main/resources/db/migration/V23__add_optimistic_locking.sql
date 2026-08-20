-- CORRECTIF AUDIT (Prompt 07 §36/§65) : aucun mécanisme de verrouillage optimiste
-- n'existait sur les entités les plus exposées à une édition concurrente (deux
-- utilisateurs modifiant le même dossier ou la même facture en même temps), risquant un
-- écrasement silencieux (last-write-wins). Ajout d'une colonne `version` gérée
-- automatiquement par Hibernate (@Version) : toute mise à jour concurrente sur une version
-- obsolète lève désormais une erreur explicite (409 CONFLICT) plutôt que d'écraser
-- silencieusement les modifications d'un autre utilisateur.
ALTER TABLE dossiers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE factures ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
