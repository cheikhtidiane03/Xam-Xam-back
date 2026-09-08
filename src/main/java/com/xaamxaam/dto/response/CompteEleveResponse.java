package com.xaamxaam.dto.response;

import com.xaamxaam.domain.user.Eleve;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Compte eleve tel que visible par l'administrateur d'etablissement ou l'enseignant")
public record CompteEleveResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        Eleve.NiveauScolaire niveauScolaire,
        boolean actif
) {
}
