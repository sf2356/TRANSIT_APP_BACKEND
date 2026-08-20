CREATE TABLE audit_logs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id    UUID NOT NULL,
    utilisateur_id   UUID,
    action           VARCHAR(50) NOT NULL,
    entite_type      VARCHAR(50) NOT NULL,
    entite_id        UUID NOT NULL,
    ancienne_valeur  JSONB,
    nouvelle_valeur  JSONB,
    adresse_ip       VARCHAR(45),
    user_agent       VARCHAR(255),
    date_action      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id)
);

CREATE INDEX idx_audit_entite ON audit_logs (entreprise_id, entite_type, entite_id);
CREATE INDEX idx_audit_date ON audit_logs (date_action);
