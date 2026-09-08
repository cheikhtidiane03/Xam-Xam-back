-- ============================================================
-- Consentement parental et conformite protection des donnees (mineurs)
-- ============================================================

ALTER TABLE utilisateurs ADD COLUMN email_verifie BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE consentements_parentaux (
    id                                    UUID PRIMARY KEY,
    eleve_id                              UUID NOT NULL UNIQUE REFERENCES eleves (id) ON DELETE CASCADE,
    parent_id                             UUID REFERENCES parents (id),
    consentement_donnees                  BOOLEAN NOT NULL DEFAULT FALSE,
    date_consentement_donnees             TIMESTAMP,
    consentement_statistiques_nominatives BOOLEAN NOT NULL DEFAULT FALSE,
    date_consentement_statistiques        TIMESTAMP,
    date_maj                              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_consentements_eleve ON consentements_parentaux (eleve_id);
