package com.xaamxaam.service.exercice;

import com.xaamxaam.domain.exercice.Badge;
import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.Reformulation;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Role;
import com.xaamxaam.repository.BadgeRepository;
import com.xaamxaam.repository.EleveBadgeRepository;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ExerciceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock private BadgeRepository badgeRepository;
    @Mock private EleveBadgeRepository eleveBadgeRepository;
    @Mock private EleveRepository eleveRepository;
    @Mock private ExerciceRepository exerciceRepository;

    private BadgeService badgeService;
    private final UUID eleveId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        badgeService = new BadgeService(badgeRepository, eleveBadgeRepository, eleveRepository, exerciceRepository);
    }

    private Eleve unEleve() {
        return Eleve.builder().id(eleveId).nom("Ba").prenom("Cheikh").email("c@exemple.com")
                .motDePasse("hash").role(Role.ELEVE).actif(true)
                .niveauScolaire(Eleve.NiveauScolaire.TERMINALE).build();
    }

    private Exercice exerciceTermine(int score) {
        Exercice exercice = Exercice.builder().id(UUID.randomUUID()).eleve(unEleve())
                .enonce("x").matiere("Maths").niveau("TERMINALE")
                .statut(Exercice.StatutExercice.TERMINE).build();
        Reformulation reformulation = Reformulation.builder().id(UUID.randomUUID())
                .exercice(exercice).texte("texte").score(score).build();
        exercice.setReformulation(reformulation);
        return exercice;
    }

    @Test
    void evaluerBadges_attribuePremierPasApresUnSeulExerciceTermine() {
        when(exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId))
                .thenReturn(List.of(exerciceTermine(70)));
        when(eleveBadgeRepository.existsByEleveIdAndBadgeCode(eq(eleveId), any())).thenReturn(false);
        when(badgeRepository.findByCode("PREMIER_PAS"))
                .thenReturn(Optional.of(Badge.builder().id(UUID.randomUUID()).code("PREMIER_PAS")
                        .nom("Premier pas").description("...").seuil(1).build()));
        when(eleveRepository.findById(eleveId)).thenReturn(Optional.of(unEleve()));

        badgeService.evaluerBadges(eleveId);

        verify(eleveBadgeRepository).save(argThat(eb -> eb.getBadge().getCode().equals("PREMIER_PAS")));
        // APPRENTI (seuil 5) ne doit pas etre attribue avec un seul exercice
        verify(badgeRepository, never()).findByCode("APPRENTI");
    }

    @Test
    void evaluerBadges_neReattribuePasUnBadgeDejaObtenu() {
        when(exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId))
                .thenReturn(List.of(exerciceTermine(70)));
        when(eleveBadgeRepository.existsByEleveIdAndBadgeCode(eleveId, "PREMIER_PAS")).thenReturn(true);

        badgeService.evaluerBadges(eleveId);

        verify(eleveBadgeRepository, never()).save(any());
    }

    @Test
    void evaluerBadges_nAttribueMaitreReformulationQueSiScoreMoyenAtteint80SurAuMoins5Exercices() {
        List<Exercice> exercices = List.of(
                exerciceTermine(90), exerciceTermine(85), exerciceTermine(80), exerciceTermine(75), exerciceTermine(95)
        );
        when(exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId)).thenReturn(exercices);
        when(eleveBadgeRepository.existsByEleveIdAndBadgeCode(eq(eleveId), any())).thenReturn(false);
        when(badgeRepository.findByCode(any())).thenReturn(Optional.of(
                Badge.builder().id(UUID.randomUUID()).code("X").nom("X").description("...").build()
        ));
        when(eleveRepository.findById(eleveId)).thenReturn(Optional.of(unEleve()));

        badgeService.evaluerBadges(eleveId);

        verify(badgeRepository).findByCode("MAITRE_REFORMULATION");
    }
}
