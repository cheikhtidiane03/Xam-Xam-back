package com.xaamxaam.service.exercice;

import com.xaamxaam.domain.exercice.ExerciceType;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.dto.request.CreerExerciceTypeRequest;
import com.xaamxaam.dto.response.ExerciceTypeResponse;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.ExerciceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Bibliotheque d'exercices types par niveau/matiere (cf. section 4.2 - V2).
 * Un exercice type sert de modele que l'eleve peut importer pour demarrer
 * rapidement un parcours guide sans avoir a saisir tout l'enonce lui-meme.
 */
@Service
@RequiredArgsConstructor
public class ExerciceTypeService {

    private final ExerciceTypeRepository exerciceTypeRepository;

    @Transactional
    public ExerciceTypeResponse creer(Utilisateur auteur, CreerExerciceTypeRequest request) {
        ExerciceType exerciceType = ExerciceType.builder()
                .titre(request.titre())
                .enonce(request.enonce())
                .matiere(request.matiere())
                .niveau(request.niveau())
                .creePar(auteur)
                .build();

        return versReponse(exerciceTypeRepository.save(exerciceType));
    }

    public List<ExerciceTypeResponse> rechercher(String matiere, String niveau) {
        List<ExerciceType> resultats;
        if (matiere != null && !matiere.isBlank() && niveau != null && !niveau.isBlank()) {
            resultats = exerciceTypeRepository.findByMatiereIgnoreCaseAndNiveauIgnoreCase(matiere, niveau);
        } else if (niveau != null && !niveau.isBlank()) {
            resultats = exerciceTypeRepository.findByNiveauIgnoreCase(niveau);
        } else {
            resultats = exerciceTypeRepository.findAll();
        }
        return resultats.stream().map(this::versReponse).toList();
    }

    public ExerciceType recuperer(UUID exerciceTypeId) {
        return exerciceTypeRepository.findById(exerciceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice type introuvable"));
    }

    private ExerciceTypeResponse versReponse(ExerciceType exerciceType) {
        return new ExerciceTypeResponse(
                exerciceType.getId(), exerciceType.getTitre(), exerciceType.getEnonce(),
                exerciceType.getMatiere(), exerciceType.getNiveau()
        );
    }
}
