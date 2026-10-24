package com.xaamxaam.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vue allegee d'un utilisateur pour le panneau Superadmin.
 * N'expose jamais le mot de passe ni d'autres champs sensibles.
 */
public record UtilisateurListeResponse(
        UUID id,
        String prenom,
        String nom,
        String email,
        String role,
        boolean actif,
        LocalDateTime dateInscription,
        LocalDateTime derniereActivite,
        long nombreExercices
) {
}
