package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vue globale du projet pour le superadmin")
public record VueGlobaleResponse(
        long nombreUtilisateursTotal,
        long nombreElevesActifs,
        long nombreEtablissementsValides,
        long nombreEtablissementsEnAttente,
        long nombreExercicesAujourdHui
) {
}
