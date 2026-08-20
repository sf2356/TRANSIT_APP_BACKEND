CREATE TABLE charges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id   UUID NOT NULL,
    dossier_id      UUID NOT NULL,
    fournisseur_id  UUID,
    libelle         VARCHAR(255) NOT NULL,
    type            VARCHAR(30) NOT NULL,
    categorie       VARCHAR(50),
    montant         NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    devise          VARCHAR(10) NOT NULL,
    statut          VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    date_charge     DATE NOT NULL DEFAULT CURRENT_DATE,
    reference       VARCHAR(100),
    notes           TEXT,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_charge_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_charge_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_charge_fournisseur FOREIGN KEY (entreprise_id, fournisseur_id) REFERENCES tiers (entreprise_id, id),
    CONSTRAINT fk_charge_created_by FOREIGN KEY (created_by) REFERENCES utilisateurs (id)
);

CREATE INDEX idx_charges_entreprise ON charges (entreprise_id);
CREATE INDEX idx_charges_dossier ON charges (dossier_id);
CREATE INDEX idx_charges_statut ON charges (statut);
CREATE INDEX idx_charges_type ON charges (type);
