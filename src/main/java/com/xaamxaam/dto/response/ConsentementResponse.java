package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Etat du consentement parental pour un eleve")
public record ConsentementResponse(
        UUID eleveId,
        boolean consentementDonnees,
        LocalDateTime dateConsentementDonnees,
        boolean consentementStatistiquesNominatives,
        LocalDateTime dateConsentementStatistiques
) {
}
