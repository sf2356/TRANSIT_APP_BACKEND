CREATE TABLE paiements (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id  UUID NOT NULL,
    numero         VARCHAR(50) NOT NULL,
    facture_id     UUID,
    cotation_id    UUID,
    dossier_id     UUID,
    client_id      UUID NOT NULL,
    montant        NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    devise         VARCHAR(10) NOT NULL,
    mode_paiement  VARCHAR(20) NOT NULL,
    date_paiement  DATE NOT NULL DEFAULT CURRENT_DATE,
    reference      VARCHAR(100),
    statut         VARCHAR(20) NOT NULL DEFAULT 'VALIDE',
    observations   TEXT,
    created_by     UUID NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT fk_paiement_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_paiement_facture FOREIGN KEY (facture_id) REFERENCES factures (id),
    CONSTRAINT fk_paiement_cotation FOREIGN KEY (cotation_id) REFERENCES cotations (id),
    CONSTRAINT fk_paiement_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_paiement_client FOREIGN KEY (entreprise_id, client_id) REFERENCES tiers (entreprise_id, id),
    CONSTRAINT fk_paiement_created_by FOREIGN KEY (created_by) REFERENCES utilisateurs (id),
    CONSTRAINT uq_paiement_numero UNIQUE (entreprise_id, numero),
    CONSTRAINT chk_paiement_au_moins_un_lien CHECK (facture_id IS NOT NULL OR cotation_id IS NOT NULL OR dossier_id IS NOT NULL)
);

CREATE INDEX idx_paiements_entreprise ON paiements (entreprise_id);
CREATE INDEX idx_paiements_facture ON paiements (facture_id);
CREATE INDEX idx_paiements_dossier ON paiements (dossier_id);
