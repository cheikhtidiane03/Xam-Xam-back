package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mise a jour du consentement parental pour un eleve")
public record ConsentementRequest(
        @NotNull Boolean consentementDonnees,
        @NotNull Boolean consentementStatistiquesNominatives
) {
}
