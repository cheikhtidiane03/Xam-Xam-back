-- Liste noire des JWT revoques (logout explicite). Nettoyee periodiquement
-- par un job planifie qui supprime les entrees dont le token est deja expire.
CREATE TABLE tokens_revoques (
    id               UUID PRIMARY KEY,
    jti              VARCHAR(255) NOT NULL UNIQUE,
    date_expiration  TIMESTAMP NOT NULL,
    date_revocation  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tokens_revoques_jti ON tokens_revoques (jti);
