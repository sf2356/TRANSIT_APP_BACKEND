-- Pas de colonne entreprise_id ici (cf. modèle validé Prompt 02 §17) : le tenant est
-- toujours dérivé via dossier_id. Toute lecture/écriture passe par DossierService pour
-- garantir l'isolation (voir MarchandiseService).
CREATE TABLE marchandises (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id            UUID NOT NULL,
    designation           VARCHAR(255) NOT NULL,
    description           TEXT,
    type_marchandise      VARCHAR(50),
    statut                VARCHAR(20) NOT NULL DEFAULT 'DECLAREE',
    nombre_colis          INTEGER CHECK (nombre_colis IS NULL OR nombre_colis >= 0),
    type_colis            VARCHAR(50),
    poids_brut            NUMERIC(12,3) CHECK (poids_brut IS NULL OR poids_brut >= 0),
    volume_total          NUMERIC(12,3) CHECK (volume_total IS NULL OR volume_total >= 0),
    numero_conteneur      VARCHAR(50),
    type_conteneur        VARCHAR(20),
    plomb                 VARCHAR(50),
    origine               VARCHAR(255),
    destination           VARCHAR(255),
    nature_marchandise    VARCHAR(255),
    marque_reference      VARCHAR(100),
    valeur_declaree       NUMERIC(15,2) CHECK (valeur_declaree IS NULL OR valeur_declaree >= 0),
    devise_valeur         VARCHAR(10),
    code_sh               VARCHAR(20),
    pays_origine          VARCHAR(100),
    pays_provenance       VARCHAR(100),
    destination_finale    VARCHAR(255),
    observations          TEXT,
    observations_douane   TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_marchandise_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id) ON DELETE CASCADE
);

CREATE INDEX idx_marchandises_dossier ON marchandises (dossier_id);
