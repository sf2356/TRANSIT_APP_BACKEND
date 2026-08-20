CREATE TABLE tiers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id       UUID NOT NULL,
    raison_sociale      VARCHAR(255) NOT NULL,
    nom_contact         VARCHAR(255),
    type                VARCHAR(20) NOT NULL,
    telephone           VARCHAR(50),
    email               VARCHAR(255),
    adresse             VARCHAR(500),
    ville               VARCHAR(100),
    pays                VARCHAR(100),
    identifiant_fiscal  VARCHAR(100),
    registre_commerce   VARCHAR(100),
    statut              VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT fk_tiers_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT uq_tiers_entreprise_id UNIQUE (entreprise_id, id)
);

CREATE INDEX idx_tiers_entreprise ON tiers (entreprise_id);
CREATE INDEX idx_tiers_type ON tiers (type);
