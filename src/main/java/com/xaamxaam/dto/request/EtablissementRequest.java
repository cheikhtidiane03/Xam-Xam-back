package com.xaamxaam.dto.request;

import com.xaamxaam.domain.etablissement.Etablissement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Creation ou mise a jour d'un etablissement (reserve au Superadmin)")
public record EtablissementRequest(
        @NotBlank String nom,
        @NotNull Etablissement.TypeEtablissement type,
        String adresse
) {
}
