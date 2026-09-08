package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statistique nominative d'un eleve, visible uniquement si le consentement parental a ete donne")
public record EleveStatNominativeResponse(
        UUID eleveId,
        String nomComplet,
        long nombreExercicesTermines,
        double scoreMoyenReformulation
) {
}
