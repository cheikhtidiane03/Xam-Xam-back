package com.xaamxaam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Creation d'un compte eleve par l'administrateur de son etablissement "
        + "(l'etablissement est deduit automatiquement du compte admin connecte, jamais fourni par le client)")
public record CreerEleveParAdminRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres") String motDePasse,
        @NotBlank @Schema(description = "SIXIEME, CINQUIEME, QUATRIEME, TROISIEME, SECONDE, PREMIERE, TERMINALE, "
                + "LICENCE_1, LICENCE_2, LICENCE_3, MASTER_1, MASTER_2 ou DOCTORAT")
        String niveauScolaire
) {
}
