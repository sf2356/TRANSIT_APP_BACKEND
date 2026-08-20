-- entite_id est une référence polymorphe SANS FK physique (elle peut pointer vers dossiers,
-- factures, cotations, paiements ou charges selon entite_type). Décision documentée et
-- assumée dans le modèle validé au Prompt 02 §44 : l'intégrité est garantie côté
-- applicatif (ValidationService), pas par une contrainte PostgreSQL.
CREATE TABLE validations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id  UUID NOT NULL,
    type           VARCHAR(50) NOT NULL,
    entite_type    VARCHAR(50) NOT NULL,
    entite_id      UUID NOT NULL,
    demandeur_id   UUID NOT NULL,
    validateur_id  UUID,
    statut         VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire    TEXT,
    date_demande   TIMESTAMPTZ NOT NULL DEFAULT now(),
    date_decision  TIMESTAMPTZ,
    CONSTRAINT fk_validation_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_validation_demandeur FOREIGN KEY (demandeur_id) REFERENCES utilisateurs (id),
    CONSTRAINT fk_validation_validateur FOREIGN KEY (validateur_id) REFERENCES utilisateurs (id)
);

CREATE INDEX idx_validations_entite ON validations (entreprise_id, entite_type, entite_id);
CREATE INDEX idx_validations_statut ON validations (statut);
