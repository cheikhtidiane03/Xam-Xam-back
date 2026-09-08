package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Creation d'un compte enseignant par l'administrateur de son etablissement "
        + "(l'etablissement est deduit automatiquement du compte admin connecte, jamais fourni par le client)")
public record CreerEnseignantParAdminRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres") String motDePasse,
        @NotBlank String matiere
) {
}
