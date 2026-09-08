CREATE TABLE tokens_reinitialisation_mdp (
    id                UUID PRIMARY KEY,
    utilisateur_id    UUID NOT NULL REFERENCES utilisateurs (id) ON DELETE CASCADE,
    token             VARCHAR(255) NOT NULL UNIQUE,
    date_expiration   TIMESTAMP NOT NULL,
    utilise           BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE tokens_verification_email (
    id                UUID PRIMARY KEY,
    utilisateur_id    UUID NOT NULL REFERENCES utilisateurs (id) ON DELETE CASCADE,
    token             VARCHAR(255) NOT NULL UNIQUE,
    date_expiration   TIMESTAMP NOT NULL,
    utilise           BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reset_token ON tokens_reinitialisation_mdp (token);
CREATE INDEX idx_verif_email_token ON tokens_verification_email (token);
