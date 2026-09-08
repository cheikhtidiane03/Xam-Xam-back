package com.xaamxaam.service.exercice;

import com.xaamxaam.config.ClaudeApiProperties;
import com.xaamxaam.domain.exercice.QuotaIa;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Role;
import com.xaamxaam.exception.QuotaDepasseException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.QuotaIaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotaIaServiceTest {

    @Mock
    private QuotaIaRepository quotaIaRepository;

    @Mock
    private EleveRepository eleveRepository;

    private QuotaIaService quotaIaService;

    private final UUID eleveId = UUID.randomUUID();
    private static final int QUOTA_MAX = 5;

    @BeforeEach
    void setUp() {
        ClaudeApiProperties properties = new ClaudeApiProperties(
                "test-key", "https://api.anthropic.com/v1/messages", "claude-sonnet-5",
                "2023-06-01", 1024, QUOTA_MAX
        );
        quotaIaService = new QuotaIaService(quotaIaRepository, eleveRepository, properties);
    }

    private Eleve unEleve() {
        return Eleve.builder().id(eleveId).nom("Ba").prenom("Cheikh").email("c@exemple.com")
                .motDePasse("hash").role(Role.ELEVE).actif(true)
                .niveauScolaire(Eleve.NiveauScolaire.TERMINALE).build();
    }

    @Test
    void verifierEtIncrementer_creeUnNouveauQuotaSiAucunPourAujourdHui() {
        when(quotaIaRepository.findForUpdateByEleveIdAndDate(eq(eleveId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(eleveRepository.findById(eleveId)).thenReturn(Optional.of(unEleve()));
        when(quotaIaRepository.save(any(QuotaIa.class))).thenAnswer(inv -> inv.getArgument(0));

        quotaIaService.verifierEtIncrementer(eleveId);

        verify(quotaIaRepository).save(argThat(q -> q.getNombreRequetes() == 1));
    }

    @Test
    void verifierEtIncrementer_incrementeUnQuotaExistantSousLaLimite() {
        QuotaIa quotaExistant = QuotaIa.builder()
                .id(UUID.randomUUID()).eleve(unEleve()).date(LocalDate.now()).nombreRequetes(2).build();
        when(quotaIaRepository.findForUpdateByEleveIdAndDate(eq(eleveId), any(LocalDate.class)))
                .thenReturn(Optional.of(quotaExistant));
        when(quotaIaRepository.save(any(QuotaIa.class))).thenAnswer(inv -> inv.getArgument(0));

        quotaIaService.verifierEtIncrementer(eleveId);

        verify(quotaIaRepository).save(argThat(q -> q.getNombreRequetes() == 3));
    }

    @Test
    void verifierEtIncrementer_leveExceptionQuandLeQuotaEstAtteint() {
        QuotaIa quotaAtteint = QuotaIa.builder()
                .id(UUID.randomUUID()).eleve(unEleve()).date(LocalDate.now()).nombreRequetes(QUOTA_MAX).build();
        when(quotaIaRepository.findForUpdateByEleveIdAndDate(eq(eleveId), any(LocalDate.class)))
                .thenReturn(Optional.of(quotaAtteint));

        assertThatThrownBy(() -> quotaIaService.verifierEtIncrementer(eleveId))
                .isInstanceOf(QuotaDepasseException.class);

        verify(quotaIaRepository, never()).save(any());
    }

    @Test
    void consulterRequetesRestantes_calculeLaDifferenceAvecLeMaximum() {
        QuotaIa quota = QuotaIa.builder()
                .id(UUID.randomUUID()).eleve(unEleve()).date(LocalDate.now()).nombreRequetes(2).build();
        when(quotaIaRepository.findByEleveIdAndDate(eq(eleveId), any(LocalDate.class)))
                .thenReturn(Optional.of(quota));

        int restantes = quotaIaService.consulterRequetesRestantes(eleveId);

        assertThat(restantes).isEqualTo(QUOTA_MAX - 2);
    }

    @Test
    void consulterRequetesRestantes_retourneLeMaximumSiAucuneRequeteAujourdHui() {
        when(quotaIaRepository.findByEleveIdAndDate(eq(eleveId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        int restantes = quotaIaService.consulterRequetesRestantes(eleveId);

        assertThat(restantes).isEqualTo(QUOTA_MAX);
    }
}
