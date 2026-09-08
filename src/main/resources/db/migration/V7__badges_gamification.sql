CREATE TABLE badges (
    id            UUID PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    nom           VARCHAR(255) NOT NULL,
    description   VARCHAR(500) NOT NULL,
    seuil         INTEGER
);

CREATE TABLE eleve_badges (
    id                UUID PRIMARY KEY,
    eleve_id          UUID NOT NULL REFERENCES eleves (id) ON DELETE CASCADE,
    badge_id          UUID NOT NULL REFERENCES badges (id),
    date_obtention    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_eleve_badge UNIQUE (eleve_id, badge_id)
);

-- Catalogue de badges du MVP (seuils bases sur le nombre d'exercices termines
-- ou le score moyen de reformulation - cf. logique dans BadgeService).
INSERT INTO badges (id, code, nom, description, seuil) VALUES
    (gen_random_uuid(), 'PREMIER_PAS', 'Premier pas', 'A termine son tout premier exercice', 1),
    (gen_random_uuid(), 'APPRENTI', 'Apprenti assidu', 'A termine 5 exercices', 5),
    (gen_random_uuid(), 'CONFIRME', 'Elève confirme', 'A termine 20 exercices', 20),
    (gen_random_uuid(), 'MAITRE_REFORMULATION', 'Maitre de la reformulation', 'Score moyen de reformulation >= 80 sur au moins 5 exercices', 80);
