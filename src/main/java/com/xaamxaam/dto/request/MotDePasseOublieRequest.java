package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Demande de reinitialisation de mot de passe")
public record MotDePasseOublieRequest(
        @NotBlank @Email String email
) {
}
