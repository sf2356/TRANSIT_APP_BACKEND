CREATE TABLE factures (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id  UUID NOT NULL,
    numero         VARCHAR(50) NOT NULL,
    type_document  VARCHAR(20) NOT NULL DEFAULT 'FACTURE',
    client_id      UUID NOT NULL,
    dossier_id     UUID,
    cotation_id    UUID,
    titre          VARCHAR(255),
    date_document  DATE NOT NULL DEFAULT CURRENT_DATE,
    date_echeance  DATE,
    devise         VARCHAR(10) NOT NULL,
    statut         VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    montant_ht     NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (montant_ht >= 0),
    montant_taxe   NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (montant_taxe >= 0),
    montant_total  NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (montant_total >= 0),
    montant_paye   NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (montant_paye >= 0),
    reste_a_payer  NUMERIC(15,2) NOT NULL DEFAULT 0,
    notes          TEXT,
    conditions     TEXT,
    created_by     UUID NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT fk_facture_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_facture_client FOREIGN KEY (entreprise_id, client_id) REFERENCES tiers (entreprise_id, id),
    CONSTRAINT fk_facture_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_facture_cotation FOREIGN KEY (cotation_id) REFERENCES cotations (id),
    CONSTRAINT fk_facture_created_by FOREIGN KEY (created_by) REFERENCES utilisateurs (id),
    CONSTRAINT uq_facture_numero UNIQUE (entreprise_id, numero)
);

CREATE INDEX idx_factures_entreprise ON factures (entreprise_id);
CREATE INDEX idx_factures_client ON factures (client_id);
CREATE INDEX idx_factures_dossier ON factures (dossier_id);
CREATE INDEX idx_factures_statut ON factures (statut);

CREATE TABLE lignes_facture (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facture_id       UUID NOT NULL,
    categorie_frais  VARCHAR(50),
    description      VARCHAR(500) NOT NULL,
    quantite         NUMERIC(12,2) NOT NULL DEFAULT 1 CHECK (quantite > 0),
    prix_unitaire    NUMERIC(15,2) NOT NULL CHECK (prix_unitaire >= 0),
    montant          NUMERIC(15,2) NOT NULL DEFAULT 0,
    taux_taxe        NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (taux_taxe >= 0),
    montant_taxe     NUMERIC(15,2) NOT NULL DEFAULT 0,
    ordre            INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_ligne_facture_facture FOREIGN KEY (facture_id) REFERENCES factures (id) ON DELETE CASCADE
);

CREATE INDEX idx_lignes_facture_facture ON lignes_facture (facture_id);

-- Complète le FK laissé en attente en V12 (documents.facture_id) maintenant que factures existe.
ALTER TABLE documents ADD CONSTRAINT fk_document_facture FOREIGN KEY (facture_id) REFERENCES factures (id);
