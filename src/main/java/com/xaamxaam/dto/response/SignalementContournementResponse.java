package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Signalement de contournement genere automatiquement, a revoir par le superadmin")
public record SignalementContournementResponse(
        UUID id,
        UUID eleveId,
        String eleveNomComplet,
        UUID exerciceId,
        String type,
        String details,
        boolean traite,
        LocalDateTime dateCreation
) {
}
