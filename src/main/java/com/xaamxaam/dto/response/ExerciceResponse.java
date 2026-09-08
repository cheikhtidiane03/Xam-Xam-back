package com.xaamxaam.dto.response;

import com.xaamxaam.domain.exercice.Exercice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Representation d'un exercice pour l'eleve")
public record ExerciceResponse(
        UUID id,
        String enonce,
        String matiere,
        String niveau,
        Exercice.StatutExercice statut,
        LocalDateTime dateCreation
) {
}
