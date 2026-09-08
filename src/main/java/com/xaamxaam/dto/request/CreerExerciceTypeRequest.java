package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Ajout d'un exercice type a la bibliotheque (enseignant ou admin d'etablissement)")
public record CreerExerciceTypeRequest(
        @NotBlank String titre,
        @NotBlank String enonce,
        @NotBlank String matiere,
        @NotBlank String niveau
) {
}
