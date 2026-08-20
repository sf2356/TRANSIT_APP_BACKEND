-- Historique métier dédié au dossier (timeline Web/Mobile).
-- Complémentaire à audit_logs : structuré spécifiquement pour l'affichage chronologique d'un dossier.
CREATE TABLE dossier_historique (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id UUID NOT NULL,
    dossier_id   UUID NOT NULL,
    utilisateur_id UUID,
    evenement    VARCHAR(50) NOT NULL, -- DOSSIER_CREATED, STATUS_CHANGED, RESPONSABLE_CHANGED, ...
    description  VARCHAR(500),
    metadata     JSONB,
    date_evenement TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_hist_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_hist_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id)
);

CREATE INDEX idx_hist_dossier ON dossier_historique (dossier_id, date_evenement);
