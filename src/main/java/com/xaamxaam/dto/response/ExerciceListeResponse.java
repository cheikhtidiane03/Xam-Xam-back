package com.xaamxaam.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne de la liste des exercices pour le panneau Superadmin. */
public record ExerciceListeResponse(
        UUID id,
        UUID eleveId,
        String eleveNomComplet,
        LocalDateTime dateCreation
) {
}
