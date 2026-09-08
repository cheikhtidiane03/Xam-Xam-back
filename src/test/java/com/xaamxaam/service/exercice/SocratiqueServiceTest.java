package com.xaamxaam.service.exercice;

import com.xaamxaam.client.ClaudeApiClient;
import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.Reformulation;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocratiqueServiceTest {

    @Mock
    private ClaudeApiClient claudeApiClient;

    private SocratiqueService socratiqueService;

    @Test
    void genererIndice_incluToujoursLaRegleDeNeJamaisDonnerLaReponseFinale() {
        socratiqueService = new SocratiqueService(claudeApiClient);
        Exercice exercice = unExercice();
        when(claudeApiClient.envoyerMessage(anyString(), anyString())).thenReturn("Indice genere");

        socratiqueService.genererIndice(exercice, 1);

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(claudeApiClient).envoyerMessage(systemPromptCaptor.capture(), anyString());

        assertThat(systemPromptCaptor.getValue())
                .contains("ne donnes JAMAIS la reponse finale")
                .contains("niveau 1 sur 3");
    }

    @Test
    void genererIndice_adapteLeNiveauDIndiceDemandeDansLePrompt() {
        socratiqueService = new SocratiqueService(claudeApiClient);
        Exercice exercice = unExercice();
        when(claudeApiClient.envoyerMessage(anyString(), anyString())).thenReturn("Indice");

        socratiqueService.genererIndice(exercice, 3);

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(claudeApiClient).envoyerMessage(systemPromptCaptor.capture(), anyString());
        assertThat(systemPromptCaptor.getValue()).contains("niveau 3 sur 3");
    }

    @Test
    void evaluerReformulation_extraitCorrectementLeScoreDepuisUneReponseJsonValide() {
        socratiqueService = new SocratiqueService(claudeApiClient);
        Exercice exercice = unExercice();
        when(claudeApiClient.envoyerMessage(anyString(), anyString()))
                .thenReturn("{\"score\": 78, \"feedback\": \"Bonne comprehension globale.\"}");

        Reformulation resultat = socratiqueService.evaluerReformulation(exercice, "Ma reformulation");

        assertThat(resultat.getScore()).isEqualTo(78);
        assertThat(resultat.getTexte()).isEqualTo("Ma reformulation");
        assertThat(resultat.getExercice()).isEqualTo(exercice);
    }

    @Test
    void evaluerReformulation_plafonneLeScoreA100SiLaReponseIaDepasse() {
        socratiqueService = new SocratiqueService(claudeApiClient);
        Exercice exercice = unExercice();
        when(claudeApiClient.envoyerMessage(anyString(), anyString()))
                .thenReturn("{\"score\": 150, \"feedback\": \"...\"}");

        Reformulation resultat = socratiqueService.evaluerReformulation(exercice, "texte");

        assertThat(resultat.getScore()).isEqualTo(100);
    }

    @Test
    void evaluerReformulation_retourneScoreZeroSiLaReponseIaNestPasDuJsonExploitable() {
        socratiqueService = new SocratiqueService(claudeApiClient);
        Exercice exercice = unExercice();
        when(claudeApiClient.envoyerMessage(anyString(), anyString()))
                .thenReturn("Reponse inattendue sans JSON");

        Reformulation resultat = socratiqueService.evaluerReformulation(exercice, "texte");

        assertThat(resultat.getScore()).isEqualTo(0);
    }

    private Exercice unExercice() {
        Eleve eleve = Eleve.builder()
                .id(UUID.randomUUID()).nom("Ba").prenom("Cheikh").email("c@exemple.com")
                .motDePasse("hash").role(Role.ELEVE).actif(true)
                .niveauScolaire(Eleve.NiveauScolaire.TERMINALE).build();

        return Exercice.builder()
                .id(UUID.randomUUID())
                .eleve(eleve)
                .enonce("Resoudre x^2 - 4 = 0")
                .matiere("Mathematiques")
                .niveau("TERMINALE")
                .build();
    }
}
