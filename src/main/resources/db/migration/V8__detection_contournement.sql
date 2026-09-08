CREATE TABLE signalements_contournement (
    id             UUID PRIMARY KEY,
    eleve_id       UUID NOT NULL REFERENCES eleves (id) ON DELETE CASCADE,
    exercice_id    UUID REFERENCES exercices (id) ON DELETE CASCADE,
    type           VARCHAR(50) NOT NULL,
    details        VARCHAR(1000),
    traite         BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_signalements_eleve ON signalements_contournement (eleve_id);
CREATE INDEX idx_signalements_traite ON signalements_contournement (traite);
