-- Tentatives de connexion echouees, pour le blocage temporaire apres
-- plusieurs echecs (protection brute-force basique - voir LoginAttemptService).
CREATE TABLE tentatives_connexion_echouees (
    id                UUID PRIMARY KEY,
    email             VARCHAR(255) NOT NULL,
    date_tentative    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tentatives_connexion_email ON tentatives_connexion_echouees (email, date_tentative);

ALTER TABLE utilisateurs ADD COLUMN derniere_connexion TIMESTAMP;
