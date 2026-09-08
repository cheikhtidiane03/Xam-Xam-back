package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Soumission d'un nouvel exercice par l'eleve (texte libre ou texte issu d'un OCR)")
public record ExerciceRequest(
        @NotBlank String enonce,
        @NotBlank String matiere,
        @NotBlank String niveau,
        String sourceImageUrl
) {
}
