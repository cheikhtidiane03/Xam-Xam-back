package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Reformulation finale de la demarche par l'eleve (methode Feynman)")
public record ReformulationRequest(
        @NotBlank String texte
) {
}
