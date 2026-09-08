package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Confirmation de l'adresse email avec le token recu")
public record VerifierEmailRequest(
        @NotBlank String token
) {
}
