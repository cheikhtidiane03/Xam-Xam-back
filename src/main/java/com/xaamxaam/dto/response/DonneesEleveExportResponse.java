package com.xaamxaam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Export complet des donnees personnelles d'un eleve (droit d'acces / portabilite)")
public record DonneesEleveExportResponse(
        UUID eleveId,
        String nom,
        String prenom,
        String email,
        String niveauScolaire,
        LocalDateTime dateCreationCompte,
        List<ExerciceExport> exercices
) {
    public record ExerciceExport(
            UUID id,
            String enonce,
            String matiere,
            String statut,
            LocalDateTime dateCreation,
            List<String> tentatives,
            String reformulationTexte,
            Integer reformulationScore
    ) {
    }
}
