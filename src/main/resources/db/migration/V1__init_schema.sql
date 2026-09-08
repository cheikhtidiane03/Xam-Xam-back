-- ============================================================
-- Xaam-Xaam+ : schema initial (V1)
-- Strategie d'heritage JOINED pour Utilisateur -> table commune
-- "utilisateurs" + une table par sous-role.
-- ============================================================

CREATE TABLE etablissements (
    id                       UUID PRIMARY KEY,
    nom                      VARCHAR(255) NOT NULL,
    type                     VARCHAR(50)  NOT NULL,
    adresse                  VARCHAR(500),
    licence_active           BOOLEAN      NOT NULL DEFAULT FALSE,
    date_expiration_licence  DATE,
    en_attente_validation    BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE utilisateurs (
    id             UUID PRIMARY KEY,
    nom            VARCHAR(255) NOT NULL,
    prenom         VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe   VARCHAR(255) NOT NULL,
    role           VARCHAR(50)  NOT NULL,
    actif          BOOLEAN      NOT NULL DEFAULT FALSE,
    date_creation  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE eleves (
    id                UUID PRIMARY KEY REFERENCES utilisateurs (id) ON DELETE CASCADE,
    niveau_scolaire   VARCHAR(50) NOT NULL,
    etablissement_id  UUID REFERENCES etablissements (id)
);

CREATE TABLE parents (
    id UUID PRIMARY KEY REFERENCES utilisateurs (id) ON DELETE CASCADE
);

CREATE TABLE parent_eleve (
    parent_id UUID NOT NULL REFERENCES parents (id) ON DELETE CASCADE,
    eleve_id  UUID NOT NULL REFERENCES eleves (id) ON DELETE CASCADE,
    PRIMARY KEY (parent_id, eleve_id)
);

CREATE TABLE enseignants (
    id                UUID PRIMARY KEY REFERENCES utilisateurs (id) ON DELETE CASCADE,
    matiere           VARCHAR(255) NOT NULL,
    etablissement_id  UUID REFERENCES etablissements (id)
);

CREATE TABLE admins_etablissement (
    id                UUID PRIMARY KEY REFERENCES utilisateurs (id) ON DELETE CASCADE,
    etablissement_id  UUID REFERENCES etablissements (id)
);

CREATE TABLE superadmins (
    id UUID PRIMARY KEY REFERENCES utilisateurs (id) ON DELETE CASCADE
);

CREATE TABLE exercices (
    id                 UUID PRIMARY KEY,
    eleve_id           UUID NOT NULL REFERENCES eleves (id) ON DELETE CASCADE,
    enonce             TEXT NOT NULL,
    source_image_url   VARCHAR(1000),
    matiere            VARCHAR(255) NOT NULL,
    niveau             VARCHAR(50)  NOT NULL,
    statut             VARCHAR(50)  NOT NULL DEFAULT 'EN_COURS',
    date_creation      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE tentatives (
    id             UUID PRIMARY KEY,
    exercice_id    UUID NOT NULL REFERENCES exercices (id) ON DELETE CASCADE,
    contenu        TEXT NOT NULL,
    relance_ia     TEXT,
    indice_niveau  INTEGER NOT NULL DEFAULT 0,
    date_heure     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE indices (
    id               UUID PRIMARY KEY,
    exercice_id      UUID NOT NULL REFERENCES exercices (id) ON DELETE CASCADE,
    niveau           INTEGER NOT NULL,
    contenu          TEXT,
    debloque         BOOLEAN NOT NULL DEFAULT FALSE,
    date_deblocage   TIMESTAMP,
    CONSTRAINT uq_indice_exercice_niveau UNIQUE (exercice_id, niveau)
);

CREATE TABLE reformulations (
    id             UUID PRIMARY KEY,
    exercice_id    UUID NOT NULL UNIQUE REFERENCES exercices (id) ON DELETE CASCADE,
    texte          TEXT NOT NULL,
    score          INTEGER NOT NULL,
    feedback_ia    TEXT,
    date_heure     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE abonnements (
    id                UUID PRIMARY KEY,
    type              VARCHAR(50) NOT NULL,
    parent_id         UUID REFERENCES parents (id),
    etablissement_id  UUID REFERENCES etablissements (id),
    date_debut        DATE NOT NULL,
    date_fin          DATE,
    statut            VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE',
    montant_fcfa      BIGINT,
    date_creation     TIMESTAMP NOT NULL DEFAULT now()
);

-- Index utiles pour les requetes les plus frequentes
CREATE INDEX idx_exercices_eleve ON exercices (eleve_id);
CREATE INDEX idx_tentatives_exercice ON tentatives (exercice_id);
CREATE INDEX idx_indices_exercice ON indices (exercice_id);
CREATE INDEX idx_eleves_etablissement ON eleves (etablissement_id);
CREATE INDEX idx_enseignants_etablissement ON enseignants (etablissement_id);
CREATE INDEX idx_utilisateurs_email ON utilisateurs (email);
