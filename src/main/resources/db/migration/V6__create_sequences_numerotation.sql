CREATE TABLE sequences_numerotation (
    entreprise_id   UUID NOT NULL,
    type_document   VARCHAR(20) NOT NULL,
    dernier_numero  BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (entreprise_id, type_document),
    CONSTRAINT fk_seq_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprises (id)
);
