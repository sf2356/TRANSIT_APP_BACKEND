CREATE TABLE roles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id UUID,
    code          VARCHAR(50) NOT NULL,
    libelle       VARCHAR(100) NOT NULL,
    est_systeme   BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_role_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id)
);
-- Unicité du code : par entreprise pour les rôles custom, globalement pour les rôles système (entreprise_id NULL)
CREATE UNIQUE INDEX uq_roles_code_entreprise ON roles (COALESCE(entreprise_id, '00000000-0000-0000-0000-000000000000'), code);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL,
    module      VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE utilisateur_roles (
    utilisateur_id UUID NOT NULL,
    role_id        UUID NOT NULL,
    PRIMARY KEY (utilisateur_id, role_id),
    CONSTRAINT fk_ur_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);
