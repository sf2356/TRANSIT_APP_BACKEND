-- Prompt 04 §41-42 : protection contre la double soumission sur les opérations financières
-- critiques (paiements en premier lieu). Une même (entreprise_id, idempotency_key, endpoint)
-- ne peut être utilisée qu'une seule fois — la contrainte UNIQUE est le véritable garde-fou,
-- le contrôle applicatif (IdempotencyService) n'étant qu'une optimisation de lecture.
CREATE TABLE idempotency_keys (
    entreprise_id     UUID NOT NULL,
    idempotency_key   VARCHAR(255) NOT NULL,
    endpoint          VARCHAR(100) NOT NULL,
    resource_type     VARCHAR(50) NOT NULL,
    resource_id       UUID NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (entreprise_id, idempotency_key, endpoint),
    CONSTRAINT fk_idempotency_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id)
);
