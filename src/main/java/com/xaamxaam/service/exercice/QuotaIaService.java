package com.xaamxaam.service.exercice;

import com.xaamxaam.config.ClaudeApiProperties;
import com.xaamxaam.domain.exercice.QuotaIa;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.exception.QuotaDepasseException;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.QuotaIaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controle du quota quotidien de requetes IA par eleve (cf. section 7 du
 * cahier des charges : maitriser le cout de l'API Claude). Appele avant
 * chaque action qui declenche un appel a SocratiqueService/ClaudeApiClient
 * (tentative, deblocage d'indice, reformulation).
 *
 * Utilise une transaction dediee (REQUIRES_NEW) + verrou pessimiste pour
 * que l'increment du compteur soit fiable meme en cas de requetes
 * concurrentes, et pour que la verification soit persistee independamment
 * du succes ou de l'echec de l'appel IA qui suit.
 */
@Service
@RequiredArgsConstructor
public class QuotaIaService {

    private final QuotaIaRepository quotaIaRepository;
    private final EleveRepository eleveRepository;
    private final ClaudeApiProperties claudeApiProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verifierEtIncrementer(UUID eleveId) {
        LocalDate aujourdHui = LocalDate.now();
        int quotaMax = claudeApiProperties.quotaRequetesJourEleve();

        QuotaIa quota = quotaIaRepository.findForUpdateByEleveIdAndDate(eleveId, aujourdHui)
                .orElseGet(() -> creerQuotaDuJour(eleveId, aujourdHui));

        if (quota.getNombreRequetes() >= quotaMax) {
            throw new QuotaDepasseException(
                    "Quota quotidien de " + quotaMax + " requetes IA atteint. Reessaie demain, "
                            + "ou continue a reflechir sur l'exercice avant de solliciter l'IA a nouveau."
            );
        }

        quota.setNombreRequetes(quota.getNombreRequetes() + 1);
        quotaIaRepository.save(quota);
    }

    public int consulterRequetesRestantes(UUID eleveId) {
        int quotaMax = claudeApiProperties.quotaRequetesJourEleve();
        int utilisees = quotaIaRepository.findByEleveIdAndDate(eleveId, LocalDate.now())
                .map(QuotaIa::getNombreRequetes)
                .orElse(0);
        return Math.max(0, quotaMax - utilisees);
    }

    private QuotaIa creerQuotaDuJour(UUID eleveId, LocalDate date) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));
        return QuotaIa.builder()
                .eleve(eleve)
                .date(date)
                .nombreRequetes(0)
                .build();
    }
}
