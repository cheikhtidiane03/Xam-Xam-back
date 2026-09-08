package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Reponse a une tentative : la relance socratique generee par l'IA (jamais la reponse finale)")
public record TentativeResponse(
        UUID tentativeId,
        String relanceIa,
        int indiceNiveauActuel,
        LocalDateTime dateHeure
) {
}
