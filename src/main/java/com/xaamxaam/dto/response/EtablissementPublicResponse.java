package com.xaamxaam.dto.response;

import com.xaamxaam.domain.etablissement.Etablissement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Etablissement tel que visible publiquement (avant connexion), pour le formulaire d'inscription")
public record EtablissementPublicResponse(
        UUID id,
        String nom,
        Etablissement.TypeEtablissement type
) {
}