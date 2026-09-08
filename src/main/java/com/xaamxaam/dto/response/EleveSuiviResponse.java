package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Tableau de bord de suivi d'un eleve (utilise par le parent et l'eleve lui-meme)")
public record EleveSuiviResponse(
        UUID eleveId,
        String nomComplet,
        long nombreExercicesTermines,
        long nombreExercicesEnCours,
        long nombreTentativesTotal,
        double scoreMoyenReformulation,
        double tempsReflexionMoyenMinutes
) {
}
