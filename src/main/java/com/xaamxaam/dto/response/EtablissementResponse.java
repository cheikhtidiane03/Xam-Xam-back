package com.xaamxaam.dto.response;

import com.xaamxaam.domain.etablissement.Etablissement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Representation d'un etablissement")
public record EtablissementResponse(
        UUID id,
        String nom,
        Etablissement.TypeEtablissement type,
        boolean licenceActive,
        LocalDate dateExpirationLicence,
        boolean enAttenteValidation
) {
}
