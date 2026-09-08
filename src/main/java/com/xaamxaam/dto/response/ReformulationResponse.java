package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resultat de l'evaluation de la reformulation finale par l'IA")
public record ReformulationResponse(
        UUID id,
        String texte,
        int score,
        String feedbackIa
) {
}
