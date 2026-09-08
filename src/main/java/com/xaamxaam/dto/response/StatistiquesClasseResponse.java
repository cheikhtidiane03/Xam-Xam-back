package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistiques agregees pour un enseignant ou un administrateur d'etablissement")
public record StatistiquesClasseResponse(
        long nombreEleves,
        long nombreExercicesTraites,
        double tauxReformulationAtteinte,
        double scoreMoyenReformulation
) {
}
