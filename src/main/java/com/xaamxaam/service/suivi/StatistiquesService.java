package com.xaamxaam.service.suivi;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.dto.response.EleveStatNominativeResponse;
import com.xaamxaam.dto.response.StatistiquesClasseResponse;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.EtablissementRepository;
import com.xaamxaam.repository.ExerciceRepository;
import com.xaamxaam.service.conformite.ConsentementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Statistiques agregees par defaut - jamais nominatives sans consentement
 * explicite (cf. section 4.2 : "statistiques de classe anonymisees ou
 * nominatives selon consentement"). La vue nominative (construireStatistiquesNominatives)
 * ne fait jamais apparaitre un eleve dont le parent n'a pas explicitement
 * consenti (ConsentementService.aConsentiAuxStatistiquesNominatives).
 */
@Service
@RequiredArgsConstructor
public class StatistiquesService {

    private final EtablissementRepository etablissementRepository;
    private final EleveRepository eleveRepository;
    private final ExerciceRepository exerciceRepository;
    private final ConsentementService consentementService;

    public StatistiquesClasseResponse construireStatistiquesEtablissement(UUID etablissementId) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement introuvable"));

        List<Eleve> eleves = eleveRepository.findByEtablissement(etablissement);

        List<Exercice> tousExercices = eleves.stream()
                .flatMap(eleve -> exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleve.getId()).stream())
                .toList();

        long total = tousExercices.size();
        long avecReformulation = tousExercices.stream()
                .filter(e -> e.getReformulation() != null)
                .count();

        double tauxReformulation = total == 0 ? 0.0 : (avecReformulation * 100.0) / total;

        double scoreMoyen = tousExercices.stream()
                .filter(e -> e.getReformulation() != null)
                .mapToInt(e -> e.getReformulation().getScore())
                .average()
                .orElse(0.0);

        return new StatistiquesClasseResponse(eleves.size(), total, tauxReformulation, scoreMoyen);
    }

    /**
     * Vue nominative, restreinte aux seuls eleves dont le parent a
     * explicitement consenti (ConsentementParental.consentementStatistiquesNominatives).
     * Les autres eleves de l'etablissement ne sont simplement pas inclus
     * dans la liste, jamais affiches de facon degradee.
     */
    public List<EleveStatNominativeResponse> construireStatistiquesNominatives(UUID etablissementId) {
        Etablissement etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement introuvable"));

        return eleveRepository.findByEtablissement(etablissement).stream()
                .filter(eleve -> consentementService.aConsentiAuxStatistiquesNominatives(eleve.getId()))
                .map(this::versStatNominative)
                .toList();
    }

    private EleveStatNominativeResponse versStatNominative(Eleve eleve) {
        List<Exercice> exercices = exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleve.getId());

        long termines = exercices.stream()
                .filter(e -> e.getStatut() == Exercice.StatutExercice.TERMINE)
                .count();
        double scoreMoyen = exercices.stream()
                .filter(e -> e.getReformulation() != null)
                .mapToInt(e -> e.getReformulation().getScore())
                .average()
                .orElse(0.0);

        return new EleveStatNominativeResponse(
                eleve.getId(), eleve.getPrenom() + " " + eleve.getNom(), termines, scoreMoyen
        );
    }
}
