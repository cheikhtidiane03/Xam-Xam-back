package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Confirmation de reinitialisation avec le token recu par email")
public record ReinitialiserMotDePasseRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres") String nouveauMotDePasse
) {
}
