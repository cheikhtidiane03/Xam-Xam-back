package com.xaamxaam.service.exercice;

import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.Indice;
import com.xaamxaam.domain.exercice.Reformulation;
import com.xaamxaam.domain.exercice.Tentative;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.dto.request.ExerciceRequest;
import com.xaamxaam.dto.request.ReformulationRequest;
import com.xaamxaam.dto.request.TentativeRequest;
import com.xaamxaam.exception.AccesRefuseException;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ExerciceRepository;
import com.xaamxaam.repository.IndiceRepository;
import com.xaamxaam.repository.TentativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciceService {

    private final ExerciceRepository exerciceRepository;
    private final EleveRepository eleveRepository;
    private final TentativeRepository tentativeRepository;
    private final IndiceRepository indiceRepository;
    private final SocratiqueService socratiqueService;
    private final QuotaIaService quotaIaService;
    private final ContournementDetectionService contournementDetectionService;

    @Transactional
    public Exercice creerExercice(UUID eleveId, ExerciceRequest request) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));

        Exercice exercice = Exercice.builder()
                .eleve(eleve)
                .enonce(request.enonce())
                .matiere(request.matiere())
                .niveau(request.niveau())
                .sourceImageUrl(request.sourceImageUrl())
                .build();

        // On ne PRE-GENERE pas le contenu des 3 indices ici : chaque appel a
        // l'API Claude a un cout (cf. section 7 du cahier des charges). Les
        // lignes Indice sont creees vides (niveau 1/2/3, non debloquees) et
        // le contenu n'est genere qu'au moment ou l'eleve debloque
        // effectivement l'indice (voir debloquerIndiceSuivant ci-dessous).
        for (int niveau = 1; niveau <= 3; niveau++) {
            exercice.getIndices().add(Indice.builder()
                    .exercice(exercice)
                    .niveau(niveau)
                    .contenu(null)
                    .debloque(false)
                    .build());
        }

        return exerciceRepository.save(exercice);
    }

    public List<Exercice> listerExercicesEleve(UUID eleveId) {
        return exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId);
    }

    public Exercice recupererExercice(UUID exerciceId, UUID eleveId) {
        Exercice exercice = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice introuvable"));
        verifierProprietaire(exercice, eleveId);
        return exercice;
    }

    @Transactional
    public Tentative soumettreTentative(UUID exerciceId, UUID eleveId, TentativeRequest request) {
        Exercice exercice = recupererExercice(exerciceId, eleveId);
        quotaIaService.verifierEtIncrementer(eleveId);

        Tentative tentative = Tentative.builder()
                .exercice(exercice)
                .contenu(request.contenu())
                .build();

        String relance = socratiqueService.genererRelanceSocratique(exercice, tentative);
        tentative.setRelanceIa(relance);

        Tentative sauvegardee = tentativeRepository.save(tentative);

        // Detection de contournement (cf. section 4.2 - V2) : n'affecte jamais
        // la reponse renvoyee a l'eleve, uniquement un signalement pour revue.
        contournementDetectionService.analyserTentative(exercice.getEleve(), exercice, sauvegardee);

        return sauvegardee;
    }

    @Transactional
    public Indice debloquerIndiceSuivant(UUID exerciceId, UUID eleveId) {
        Exercice exercice = recupererExercice(exerciceId, eleveId);
        quotaIaService.verifierEtIncrementer(eleveId);

        List<Indice> indices = indiceRepository.findByExerciceIdOrderByNiveauAsc(exerciceId);
        Indice prochainIndice = indices.stream()
                .filter(i -> !i.isDebloque())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tous les indices ont deja ete debloques pour cet exercice."));

        prochainIndice.setDebloque(true);
        prochainIndice.setDateDeblocage(java.time.LocalDateTime.now());
        // Le contenu de l'indice est genere seulement maintenant, au moment
        // reel ou l'eleve le debloque (voir remarque cout dans creerExercice).
        String contenu = socratiqueService.genererIndice(exercice, prochainIndice.getNiveau());
        prochainIndice.setContenu(contenu);
        return indiceRepository.save(prochainIndice);
    }

    @Transactional
    public Reformulation soumettreReformulation(UUID exerciceId, UUID eleveId, ReformulationRequest request) {
        Exercice exercice = recupererExercice(exerciceId, eleveId);
        quotaIaService.verifierEtIncrementer(eleveId);

        Reformulation reformulation = socratiqueService.evaluerReformulation(exercice, request.texte());
        exercice.setReformulation(reformulation);
        exercice.setStatut(Exercice.StatutExercice.TERMINE);
        exerciceRepository.save(exercice);

        return reformulation;
    }

    private void verifierProprietaire(Exercice exercice, UUID eleveId) {
        if (!exercice.getEleve().getId().equals(eleveId)) {
            throw new AccesRefuseException("Cet exercice n'appartient pas a cet eleve.");
        }
    }
}
