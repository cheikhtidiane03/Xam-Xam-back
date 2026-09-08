-- ============================================================
-- Liaison parent-eleve via code genere par l'eleve (jamais via email,
-- pour eviter qu'un inconnu se rattache a un compte de mineur juste en
-- connaissant son adresse email - voir LiaisonParentEleveService).
-- ============================================================

CREATE TABLE codes_liaison_parent_eleve (
    id               UUID PRIMARY KEY,
    eleve_id         UUID NOT NULL REFERENCES eleves (id) ON DELETE CASCADE,
    code             VARCHAR(20) NOT NULL UNIQUE,
    date_expiration  TIMESTAMP NOT NULL,
    utilise          BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_codes_liaison_code ON codes_liaison_parent_eleve (code);
