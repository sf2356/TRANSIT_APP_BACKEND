CREATE TABLE dossiers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id         UUID NOT NULL,
    numero                VARCHAR(50) NOT NULL,
    client_id             UUID NOT NULL,
    titre                 VARCHAR(255) NOT NULL,
    mode_transport         VARCHAR(30),
    priorite              VARCHAR(20) NOT NULL DEFAULT 'NORMALE',
    responsable_id        UUID,
    date_ouverture        DATE NOT NULL DEFAULT CURRENT_DATE,
    date_echeance         DATE,
    date_cloture          DATE,
    statut                VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    numero_ordre_transit  VARCHAR(100),
    date_ordre_transit    DATE,
    reference_client      VARCHAR(100),
    donneur_ordre         VARCHAR(255),
    type_operation        VARCHAR(30),
    regime_douanier       VARCHAR(30),
    incoterm              VARCHAR(10),
    origine               VARCHAR(255),
    provenance            VARCHAR(255),
    destination           VARCHAR(255),
    instructions          TEXT,
    description           TEXT,
    notes                 TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    deleted_at            TIMESTAMPTZ,
    CONSTRAINT fk_dossier_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_dossier_client FOREIGN KEY (entreprise_id, client_id) REFERENCES tiers (entreprise_id, id),
    CONSTRAINT fk_dossier_responsable FOREIGN KEY (responsable_id) REFERENCES utilisateurs (id),
    CONSTRAINT uq_dossier_numero UNIQUE (entreprise_id, numero),
    CONSTRAINT chk_dossier_dates CHECK (date_echeance IS NULL OR date_echeance >= date_ouverture)
);

CREATE INDEX idx_dossiers_entreprise ON dossiers (entreprise_id);
CREATE INDEX idx_dossiers_client ON dossiers (client_id);
CREATE INDEX idx_dossiers_responsable ON dossiers (responsable_id);
CREATE INDEX idx_dossiers_statut ON dossiers (statut);
