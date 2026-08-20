CREATE TABLE utilisateurs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id       UUID NOT NULL,
    nom                 VARCHAR(100) NOT NULL,
    prenom              VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    telephone           VARCHAR(50),
    mot_de_passe_hash   VARCHAR(255) NOT NULL,
    ville_affectation   VARCHAR(100),
    statut              VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    derniere_connexion  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT fk_utilisateur_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT uq_utilisateur_email_entreprise UNIQUE (entreprise_id, email)
);

CREATE INDEX idx_utilisateurs_entreprise ON utilisateurs (entreprise_id);
