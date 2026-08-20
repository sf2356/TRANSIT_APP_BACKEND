CREATE TABLE relances (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id       UUID NOT NULL,
    facture_id          UUID NOT NULL,
    client_id           UUID NOT NULL,
    type_relance        VARCHAR(20) NOT NULL,
    statut              VARCHAR(30) NOT NULL DEFAULT 'A_RELANCER',
    date_relance        DATE NOT NULL DEFAULT CURRENT_DATE,
    prochaine_relance   DATE,
    commentaire         TEXT,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_relance_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_relance_facture FOREIGN KEY (facture_id) REFERENCES factures (id),
    CONSTRAINT fk_relance_client FOREIGN KEY (entreprise_id, client_id) REFERENCES tiers (entreprise_id, id),
    CONSTRAINT fk_relance_created_by FOREIGN KEY (created_by) REFERENCES utilisateurs (id)
);

CREATE INDEX idx_relances_facture ON relances (facture_id);
CREATE INDEX idx_relances_entreprise ON relances (entreprise_id);
CREATE INDEX idx_relances_statut ON relances (statut);
