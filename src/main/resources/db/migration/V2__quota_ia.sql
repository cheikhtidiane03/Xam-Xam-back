-- ============================================================
-- Xaam-Xaam+ : quota de requetes IA par eleve et par jour
-- (cf. section 7 du cahier des charges - maitrise du cout API Claude)
-- ============================================================

CREATE TABLE quotas_ia (
    id               UUID PRIMARY KEY,
    eleve_id         UUID NOT NULL REFERENCES eleves (id) ON DELETE CASCADE,
    date             DATE NOT NULL,
    nombre_requetes  INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_quota_eleve_date UNIQUE (eleve_id, date)
);

CREATE INDEX idx_quotas_ia_eleve_date ON quotas_ia (eleve_id, date);
