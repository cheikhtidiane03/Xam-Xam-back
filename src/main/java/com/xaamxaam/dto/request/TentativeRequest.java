package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Reponse de l'eleve a une relance socratique de l'IA")
public record TentativeRequest(
        @NotBlank String contenu
) {
}
