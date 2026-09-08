package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Compte enseignant tel que visible par l'administrateur d'etablissement")
public record CompteEnseignantResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        String matiere,
        boolean actif
) {
}
