CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE entreprises (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom               VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    telephone         VARCHAR(50),
    adresse           VARCHAR(500),
    pays              VARCHAR(100),
    ville             VARCHAR(100),
    secteur_activite  VARCHAR(100),
    devise_defaut     VARCHAR(10) NOT NULL DEFAULT 'XOF',
    logo              VARCHAR(500),
    type_activite     VARCHAR(50),
    statut            VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_entreprises_email UNIQUE (email)
);
