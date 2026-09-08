package com.xaamxaam.dto.response;

import com.xaamxaam.domain.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reponse retournee apres connexion ou inscription reussie")
public record AuthResponse(
        String token,
        String email,
        String nom,
        String prenom,
        Role role,
        boolean emailVerifie
) {
}
