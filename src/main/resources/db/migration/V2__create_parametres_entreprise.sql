CREATE TABLE parametres_entreprise (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id        UUID NOT NULL,
    prefixe_dossier      VARCHAR(20) NOT NULL DEFAULT 'DOS',
    prefixe_cotation     VARCHAR(20) NOT NULL DEFAULT 'COT',
    prefixe_facture      VARCHAR(20) NOT NULL DEFAULT 'FAC',
    prefixe_paiement     VARCHAR(20) NOT NULL DEFAULT 'PAY',
    logo                 VARCHAR(500),
    signature_image      VARCHAR(500),
    nom_signataire       VARCHAR(255),
    fonction_signataire  VARCHAR(255),
    devise               VARCHAR(10) NOT NULL DEFAULT 'XOF',
    config_metier        JSONB NOT NULL DEFAULT '{}',
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_param_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT uq_param_entreprise UNIQUE (entreprise_id)
);
