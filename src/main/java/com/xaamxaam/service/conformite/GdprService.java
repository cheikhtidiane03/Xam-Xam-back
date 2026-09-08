package com.xaamxaam.service.conformite;

import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.Tentative;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.dto.response.DonneesEleveExportResponse;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ExerciceRepository;
import com.xaamxaam.repository.TentativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Droit d'acces (export) et droit a l'oubli (suppression) sur les donnees
 * personnelles d'un eleve, requis pour la conformite protection des
 * donnees des mineurs (cf. remarque de securite initiale sur le cahier
 * des charges).
 */
@Service
@RequiredArgsConstructor
public class GdprService {

    private final EleveRepository eleveRepository;
    private final ExerciceRepository exerciceRepository;
    private final TentativeRepository tentativeRepository;

    public DonneesEleveExportResponse exporterDonnees(UUID eleveId) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));

        List<Exercice> exercices = exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId);

        List<DonneesEleveExportResponse.ExerciceExport> exportsExercices = exercices.stream()
                .map(exercice -> {
                    List<String> tentatives = tentativeRepository.findByExerciceIdOrderByDateHeureAsc(exercice.getId())
                            .stream()
                            .map(Tentative::getContenu)
                            .toList();

                    return new DonneesEleveExportResponse.ExerciceExport(
                            exercice.getId(),
                            exercice.getEnonce(),
                            exercice.getMatiere(),
                            exercice.getStatut().name(),
                            exercice.getDateCreation(),
                            tentatives,
                            exercice.getReformulation() != null ? exercice.getReformulation().getTexte() : null,
                            exercice.getReformulation() != null ? exercice.getReformulation().getScore() : null
                    );
                })
                .toList();

        return new DonneesEleveExportResponse(
                eleve.getId(),
                eleve.getNom(),
                eleve.getPrenom(),
                eleve.getEmail(),
                eleve.getNiveauScolaire().name(),
                eleve.getDateCreation(),
                exportsExercices
        );
    }

    /**
     * Droit a l'oubli : suppression definitive du compte et de toutes les
     * donnees associees (exercices, tentatives, indices, reformulations
     * suppprimes en cascade via les contraintes ON DELETE CASCADE de la
     * migration V1). Operation irreversible.
     */
    @Transactional
    public void supprimerCompteEtDonnees(UUID eleveId) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));
        eleveRepository.delete(eleve);
    }
}
