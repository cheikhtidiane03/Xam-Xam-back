package com.xaamxaam.service.suivi;

import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.dto.response.EleveSuiviResponse;
import com.xaamxaam.dto.response.ProgressionMatiereResponse;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ExerciceRepository;
import com.xaamxaam.repository.TentativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableauDeBordService {

    private final EleveRepository eleveRepository;
    private final ExerciceRepository exerciceRepository;
    private final TentativeRepository tentativeRepository;

    public EleveSuiviResponse construireSuiviEleve(UUID eleveId) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));

        List<Exercice> exercices = exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId);

        long termines = exercices.stream()
                .filter(e -> e.getStatut() == Exercice.StatutExercice.TERMINE)
                .count();
        long enCours = exercices.stream()
                .filter(e -> e.getStatut() == Exercice.StatutExercice.EN_COURS)
                .count();

        long nombreTentativesTotal = exercices.stream()
                .mapToLong(e -> tentativeRepository.countByExerciceId(e.getId()))
                .sum();

        double scoreMoyen = exercices.stream()
                .filter(e -> e.getReformulation() != null)
                .mapToInt(e -> e.getReformulation().getScore())
                .average()
                .orElse(0.0);

        double tempsMoyenMinutes = exercices.stream()
                .filter(e -> e.getReformulation() != null)
                .mapToLong(e -> Duration.between(e.getDateCreation(), e.getReformulation().getDateHeure()).toMinutes())
                .average()
                .orElse(0.0);

        return new EleveSuiviResponse(
                eleve.getId(),
                eleve.getPrenom() + " " + eleve.getNom(),
                termines,
                enCours,
                nombreTentativesTotal,
                scoreMoyen,
                tempsMoyenMinutes
        );
    }

    /**
     * Historique et progression par matiere (cf. section 4.2 du cahier des
     * charges - V2 : "compte eleve avec historique et progression par matiere").
     */
    public List<ProgressionMatiereResponse> construireProgressionParMatiere(UUID eleveId) {
        List<Exercice> exercices = exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId);

        Map<String, List<Exercice>> parMatiere = exercices.stream()
                .collect(Collectors.groupingBy(Exercice::getMatiere));

        return parMatiere.entrySet().stream()
                .map(entry -> {
                    String matiere = entry.getKey();
                    List<Exercice> exercicesMatiere = entry.getValue();

                    long termines = exercicesMatiere.stream()
                            .filter(e -> e.getStatut() == Exercice.StatutExercice.TERMINE)
                            .count();
                    long enCours = exercicesMatiere.stream()
                            .filter(e -> e.getStatut() == Exercice.StatutExercice.EN_COURS)
                            .count();
                    double scoreMoyen = exercicesMatiere.stream()
                            .filter(e -> e.getReformulation() != null)
                            .mapToInt(e -> e.getReformulation().getScore())
                            .average()
                            .orElse(0.0);

                    return new ProgressionMatiereResponse(matiere, termines, enCours, scoreMoyen);
                })
                .sorted(Comparator.comparing(ProgressionMatiereResponse::matiere))
                .toList();
    }
}
