CREATE TABLE mouvements_caisse (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id   UUID NOT NULL,
    dossier_id      UUID,
    paiement_id     UUID,
    type_mouvement  VARCHAR(10) NOT NULL,
    categorie       VARCHAR(50),
    libelle         VARCHAR(255) NOT NULL,
    montant         NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    devise          VARCHAR(10) NOT NULL,
    mode_paiement   VARCHAR(20),
    date_mouvement  TIMESTAMPTZ NOT NULL DEFAULT now(),
    reference       VARCHAR(100),
    statut          VARCHAR(20) NOT NULL DEFAULT 'VALIDE',
    notes           TEXT,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_caisse_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_caisse_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_caisse_paiement FOREIGN KEY (paiement_id) REFERENCES paiements (id),
    CONSTRAINT fk_caisse_created_by FOREIGN KEY (created_by) REFERENCES utilisateurs (id)
);

CREATE INDEX idx_caisse_entreprise_date ON mouvements_caisse (entreprise_id, date_mouvement);
CREATE INDEX idx_caisse_paiement ON mouvements_caisse (paiement_id);
