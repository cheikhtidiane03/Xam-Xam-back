CREATE TABLE exercices_types (
    id                 UUID PRIMARY KEY,
    titre              VARCHAR(255) NOT NULL,
    enonce             TEXT NOT NULL,
    matiere            VARCHAR(255) NOT NULL,
    niveau             VARCHAR(50)  NOT NULL,
    cree_par_id        UUID REFERENCES utilisateurs (id),
    etablissement_id   UUID REFERENCES etablissements (id),
    date_creation      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_exercices_types_matiere_niveau ON exercices_types (matiere, niveau);
