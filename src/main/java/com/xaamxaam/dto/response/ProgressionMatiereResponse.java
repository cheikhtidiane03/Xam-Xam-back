package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Progression de l'eleve pour une matiere donnee")
public record ProgressionMatiereResponse(
        String matiere,
        long nombreExercicesTermines,
        long nombreExercicesEnCours,
        double scoreMoyenReformulation
) {
}
