CREATE TABLE documents (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entreprise_id    UUID NOT NULL,
    dossier_id       UUID NOT NULL,
    marchandise_id   UUID,
    facture_id       UUID,
    cotation_id      UUID,
    titre            VARCHAR(255) NOT NULL,
    type_document    VARCHAR(50) NOT NULL,
    chemin_fichier   VARCHAR(500) NOT NULL,
    nom_fichier      VARCHAR(255) NOT NULL,
    type_mime        VARCHAR(100) NOT NULL,
    taille           BIGINT NOT NULL CHECK (taille > 0),
    statut           VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    date_reception   DATE,
    date_expiration  DATE,
    ajoute_par       UUID NOT NULL,
    date_ajout       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT fk_document_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id),
    CONSTRAINT fk_document_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_document_marchandise FOREIGN KEY (marchandise_id) REFERENCES marchandises (id),
    CONSTRAINT fk_document_ajoute_par FOREIGN KEY (ajoute_par) REFERENCES utilisateurs (id)
    -- fk_document_facture / fk_document_cotation ajoutées lors des migrations créant ces tables (V13, étape 13)
);

CREATE INDEX idx_documents_dossier ON documents (dossier_id);
CREATE INDEX idx_documents_entreprise ON documents (entreprise_id);
