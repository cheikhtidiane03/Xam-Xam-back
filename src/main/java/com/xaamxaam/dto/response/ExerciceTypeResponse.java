package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Exercice type disponible dans la bibliotheque")
public record ExerciceTypeResponse(
        UUID id,
        String titre,
        String enonce,
        String matiere,
        String niveau
) {
}
