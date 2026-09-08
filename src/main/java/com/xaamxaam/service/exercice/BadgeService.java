package com.xaamxaam.service.exercice;

import com.xaamxaam.domain.exercice.Badge;
import com.xaamxaam.domain.exercice.EleveBadge;
import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.dto.response.BadgeResponse;
import com.xaamxaam.repository.BadgeRepository;
import com.xaamxaam.repository.EleveBadgeRepository;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ExerciceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gamification simple pour le MVP (cf. section 4.2 - V2 : "systeme de
 * badges / gamification de la progression"). Evalue les badges apres
 * chaque exercice termine (reformulation soumise) : appele depuis
 * ExerciceService.soumettreReformulation.
 *
 * Regles codees en dur pour le MVP (seuils definis dans la migration V7) ;
 * a terme, pourrait devenir configurable par le superadmin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final EleveBadgeRepository eleveBadgeRepository;
    private final EleveRepository eleveRepository;
    private final ExerciceRepository exerciceRepository;

    private static final String PREMIER_PAS = "PREMIER_PAS";
    private static final String APPRENTI = "APPRENTI";
    private static final String CONFIRME = "CONFIRME";
    private static final String MAITRE_REFORMULATION = "MAITRE_REFORMULATION";

    @Transactional
    public void evaluerBadges(UUID eleveId) {
        List<Exercice> exercices = exerciceRepository.findByEleveIdOrderByDateCreationDesc(eleveId);
        long nombreTermines = exercices.stream()
                .filter(e -> e.getStatut() == Exercice.StatutExercice.TERMINE)
                .count();

        attribuerSiSeuilAtteint(eleveId, PREMIER_PAS, nombreTermines >= 1);
        attribuerSiSeuilAtteint(eleveId, APPRENTI, nombreTermines >= 5);
        attribuerSiSeuilAtteint(eleveId, CONFIRME, nombreTermines >= 20);

        List<Exercice> avecReformulation = exercices.stream()
                .filter(e -> e.getReformulation() != null)
                .toList();
        if (avecReformulation.size() >= 5) {
            double scoreMoyen = avecReformulation.stream()
                    .mapToInt(e -> e.getReformulation().getScore())
                    .average()
                    .orElse(0.0);
            attribuerSiSeuilAtteint(eleveId, MAITRE_REFORMULATION, scoreMoyen >= 80);
        }
    }

    public List<BadgeResponse> listerBadgesDeLeleve(UUID eleveId) {
        return eleveBadgeRepository.findByEleveIdOrderByDateObtentionDesc(eleveId).stream()
                .map(eb -> new BadgeResponse(
                        eb.getBadge().getCode(), eb.getBadge().getNom(), eb.getBadge().getDescription(), eb.getDateObtention()
                ))
                .toList();
    }

    private void attribuerSiSeuilAtteint(UUID eleveId, String codeBadge, boolean seuilAtteint) {
        if (!seuilAtteint) {
            return;
        }
        if (eleveBadgeRepository.existsByEleveIdAndBadgeCode(eleveId, codeBadge)) {
            return; // deja obtenu, on n'attribue jamais deux fois le meme badge
        }

        Badge badge = badgeRepository.findByCode(codeBadge).orElse(null);
        if (badge == null) {
            log.warn("Badge {} reference en code mais absent en base (verifier la migration V7)", codeBadge);
            return;
        }

        Eleve eleve = eleveRepository.findById(eleveId).orElse(null);
        if (eleve == null) {
            return;
        }

        eleveBadgeRepository.save(EleveBadge.builder().eleve(eleve).badge(badge).build());
        log.info("Badge {} attribue a l'eleve {}", codeBadge, eleveId);
    }
}
