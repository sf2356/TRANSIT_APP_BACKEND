CREATE TABLE cotations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id  UUID NOT NULL,
    numero         VARCHAR(50) NOT NULL,
    client_id      UUID NOT NULL,
    dossier_id     UUID,
    titre          VARCHAR(255),
    date_cotation  DATE NOT NULL DEFAULT CURRENT_DATE,
    date_validite  DATE,
    devise         VARCHAR(10) NOT NULL,
    statut         VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    montant_ht     NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (montant_ht >= 0),
    montant_taxe   NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (montant_taxe >= 0),
    montant_total  NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (montant_total >= 0),
    notes          TEXT,
    conditions     TEXT,
    created_by     UUID NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_cotation_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_cotation_client FOREIGN KEY (entreprise_id, client_id) REFERENCES tiers (entreprise_id, id),
    CONSTRAINT fk_cotation_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_cotation_created_by FOREIGN KEY (created_by) REFERENCES utilisateurs (id),
    CONSTRAINT uq_cotation_numero UNIQUE (entreprise_id, numero)
);

CREATE INDEX idx_cotations_entreprise ON cotations (entreprise_id);
CREATE INDEX idx_cotations_dossier ON cotations (dossier_id);
CREATE INDEX idx_cotations_client ON cotations (client_id);
CREATE INDEX idx_cotations_statut ON cotations (statut);

CREATE TABLE lignes_cotation (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cotation_id      UUID NOT NULL,
    categorie_frais  VARCHAR(50),
    description      VARCHAR(500) NOT NULL,
    quantite         NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK (quantite > 0),
    prix_unitaire    NUMERIC(15,2) NOT NULL CHECK (prix_unitaire >= 0),
    montant          NUMERIC(15,2) NOT NULL DEFAULT 0,
    taux_taxe        NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (taux_taxe >= 0),
    montant_taxe     NUMERIC(15,2) NOT NULL DEFAULT 0,
    ordre            INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_ligne_cotation_cotation FOREIGN KEY (cotation_id) REFERENCES cotations (id) ON DELETE CASCADE
);

CREATE INDEX idx_lignes_cotation_cotation ON lignes_cotation (cotation_id);

-- Complète le FK laissé en attente en V12 maintenant que la table cotations existe.
ALTER TABLE documents ADD CONSTRAINT fk_document_cotation FOREIGN KEY (cotation_id) REFERENCES cotations (id);
