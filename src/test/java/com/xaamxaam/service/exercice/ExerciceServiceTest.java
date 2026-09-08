package com.xaamxaam.service.exercice;

import com.xaamxaam.domain.exercice.*;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Role;
import com.xaamxaam.dto.request.ExerciceRequest;
import com.xaamxaam.dto.request.ReformulationRequest;
import com.xaamxaam.dto.request.TentativeRequest;
import com.xaamxaam.exception.AccesRefuseException;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ExerciceRepository;
import com.xaamxaam.repository.IndiceRepository;
import com.xaamxaam.repository.TentativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciceServiceTest {

    @Mock private ExerciceRepository exerciceRepository;
    @Mock private EleveRepository eleveRepository;
    @Mock private TentativeRepository tentativeRepository;
    @Mock private IndiceRepository indiceRepository;
    @Mock private SocratiqueService socratiqueService;
    @Mock private QuotaIaService quotaIaService;
    @Mock private ContournementDetectionService contournementDetectionService;

    private ExerciceService exerciceService;

    private final UUID eleveProprietaireId = UUID.randomUUID();
    private final UUID autreEleveId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        exerciceService = new ExerciceService(
                exerciceRepository, eleveRepository, tentativeRepository, indiceRepository,
                socratiqueService, quotaIaService, contournementDetectionService
        );
    }

    private Eleve eleve(UUID id) {
        return Eleve.builder().id(id).nom("Ba").prenom("Cheikh").email(id + "@exemple.com")
                .motDePasse("hash").role(Role.ELEVE).actif(true)
                .niveauScolaire(Eleve.NiveauScolaire.TERMINALE).build();
    }

    private Exercice exerciceDe(UUID proprietaireId) {
        return Exercice.builder()
                .id(UUID.randomUUID())
                .eleve(eleve(proprietaireId))
                .enonce("Resoudre x^2 - 4 = 0")
                .matiere("Mathematiques")
                .niveau("TERMINALE")
                .build();
    }

    @Test
    void creerExercice_sauvegardeAvecTroisIndicesNonDebloquesEtNonGeneres() {
        Eleve eleve = eleve(eleveProprietaireId);
        when(eleveRepository.findById(eleveProprietaireId)).thenReturn(Optional.of(eleve));
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciceRequest request = new ExerciceRequest("Resoudre x^2 - 4 = 0", "Mathematiques", "TERMINALE", null);
        Exercice resultat = exerciceService.creerExercice(eleveProprietaireId, request);

        assertThat(resultat.getIndices()).hasSize(3);
        assertThat(resultat.getIndices()).allMatch(i -> !i.isDebloque() && i.getContenu() == null);
        // La creation d'exercice ne doit declencher AUCUN appel a l'IA
        // (chaque appel Claude a un cout, cf. section 7 du cahier des charges).
        verifyNoInteractions(socratiqueService);
        verifyNoInteractions(quotaIaService);
    }

    @Test
    void creerExercice_leveResourceNotFoundSiEleveInconnu() {
        when(eleveRepository.findById(eleveProprietaireId)).thenReturn(Optional.empty());
        ExerciceRequest request = new ExerciceRequest("enonce", "Maths", "TERMINALE", null);

        assertThatThrownBy(() -> exerciceService.creerExercice(eleveProprietaireId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recupererExercice_leveAccesRefuseSiLexerciceAppartientAUnAutreEleve() {
        Exercice exercice = exerciceDe(autreEleveId);
        when(exerciceRepository.findById(exercice.getId())).thenReturn(Optional.of(exercice));

        assertThatThrownBy(() -> exerciceService.recupererExercice(exercice.getId(), eleveProprietaireId))
                .isInstanceOf(AccesRefuseException.class);
    }

    @Test
    void recupererExercice_reussitSiLeleveEstBienLeProprietaire() {
        Exercice exercice = exerciceDe(eleveProprietaireId);
        when(exerciceRepository.findById(exercice.getId())).thenReturn(Optional.of(exercice));

        Exercice resultat = exerciceService.recupererExercice(exercice.getId(), eleveProprietaireId);

        assertThat(resultat).isEqualTo(exercice);
    }

    @Test
    void soumettreTentative_verifieLeQuotaAvantDappelerLIA() {
        Exercice exercice = exerciceDe(eleveProprietaireId);
        when(exerciceRepository.findById(exercice.getId())).thenReturn(Optional.of(exercice));
        when(socratiqueService.genererRelanceSocratique(any(), any())).thenReturn("Quelle est la premiere etape ?");
        when(tentativeRepository.save(any(Tentative.class))).thenAnswer(inv -> inv.getArgument(0));

        exerciceService.soumettreTentative(exercice.getId(), eleveProprietaireId, new TentativeRequest("x = 2"));

        var ordre = inOrder(quotaIaService, socratiqueService);
        ordre.verify(quotaIaService).verifierEtIncrementer(eleveProprietaireId);
        ordre.verify(socratiqueService).genererRelanceSocratique(any(), any());
    }

    @Test
    void soumettreTentative_refuseUnExerciceQuiNAppartientPasALeleve() {
        Exercice exercice = exerciceDe(autreEleveId);
        when(exerciceRepository.findById(exercice.getId())).thenReturn(Optional.of(exercice));

        assertThatThrownBy(() -> exerciceService.soumettreTentative(
                exercice.getId(), eleveProprietaireId, new TentativeRequest("x = 2")
        )).isInstanceOf(AccesRefuseException.class);

        verifyNoInteractions(socratiqueService);
        verifyNoInteractions(quotaIaService);
    }

    @Test
    void debloquerIndiceSuivant_debloqueLePremierIndiceNonDebloqueEtGenereSonContenu() {
        Exercice exercice = exerciceDe(eleveProprietaireId);
        Indice indice1 = Indice.builder().id(UUID.randomUUID()).exercice(exercice).niveau(1).debloque(false).build();
        Indice indice2 = Indice.builder().id(UUID.randomUUID()).exercice(exercice).niveau(2).debloque(false).build();

        when(exerciceRepository.findById(exercice.getId())).thenReturn(Optional.of(exercice));
        when(indiceRepository.findByExerciceIdOrderByNiveauAsc(exercice.getId())).thenReturn(List.of(indice1, indice2));
        when(socratiqueService.genererIndice(exercice, 1)).thenReturn("Pense a factoriser.");
        when(indiceRepository.save(any(Indice.class))).thenAnswer(inv -> inv.getArgument(0));

        Indice resultat = exerciceService.debloquerIndiceSuivant(exercice.getId(), eleveProprietaireId);

        assertThat(resultat.isDebloque()).isTrue();
        assertThat(resultat.getContenu()).isEqualTo("Pense a factoriser.");
        assertThat(resultat.getNiveau()).isEqualTo(1);
        verify(quotaIaService).verifierEtIncrementer(eleveProprietaireId);
    }

    @Test
    void debloquerIndiceSuivant_leveIllegalStateSiTousLesIndicesSontDejaDebloques() {
        Exercice exercice = exerciceDe(eleveProprietaireId);
        Indice indiceDejaDebloque = Indice.builder().id(UUID.randomUUID()).exercice(exercice).niveau(1).debloque(true).build();

        when(exerciceRepository.findById(exercice.getId())).thenReturn(Optional.of(exercice));
        when(indiceRepository.findByExerciceIdOrderByNiveauAsc(exercice.getId())).thenReturn(List.of(indiceDejaDebloque));

        assertThatThrownBy(() -> exerciceService.debloquerIndiceSuivant(exercice.getId(), eleveProprietaireId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void soumettreReformulation_clotureLexerciceEtEnregistreLevaluation() {
        Exercice exercice = exerciceDe(eleveProprietaireId);
        Reformulation reformulationEvaluee = Reformulation.builder()
                .id(UUID.randomUUID()).exercice(exercice).texte("Explication finale").score(85).build();

        when(exerciceRepository.findById(exercice.getId())).thenReturn(Optional.of(exercice));
        when(socratiqueService.evaluerReformulation(exercice, "Explication finale")).thenReturn(reformulationEvaluee);
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(inv -> inv.getArgument(0));

        Reformulation resultat = exerciceService.soumettreReformulation(
                exercice.getId(), eleveProprietaireId, new ReformulationRequest("Explication finale")
        );

        assertThat(resultat.getScore()).isEqualTo(85);
        assertThat(exercice.getStatut()).isEqualTo(Exercice.StatutExercice.TERMINE);
        assertThat(exercice.getReformulation()).isEqualTo(reformulationEvaluee);
    }
}
