package com.xaamxaam.dto.request;

import com.xaamxaam.domain.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Demande de creation de compte")
public record RegisterRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        @NotBlank @Schema(description = "Minimum 8 caracteres") String motDePasse,
        @NotNull Role role,

        @Schema(description = "Requis si role = ELEVE, ENSEIGNANT ou ADMIN_ETABLISSEMENT")
        String etablissementId,

        @Schema(description = "Requis si role = ELEVE (ex : TROISIEME, SECONDE...)")
        String niveauScolaire,

        @Schema(description = "Requis si role = ENSEIGNANT")
        String matiere
) {
}
