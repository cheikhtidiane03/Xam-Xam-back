package com.xaamxaam.service.notification;

import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Parent;
import com.xaamxaam.dto.response.EleveSuiviResponse;
import com.xaamxaam.repository.ParentRepository;
import com.xaamxaam.service.suivi.TableauDeBordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Genere et envoie le rapport hebdomadaire aux parents (cf. section 4.2 du
 * cahier des charges - V2 : "compte parent avec rapports hebdomadaires").
 *
 * Planifie chaque lundi a 8h (heure serveur). L'envoi effectif est delegue
 * a EmailService, qui ne fait jamais echouer le job global si un envoi
 * individuel echoue (voir gestion d'erreur dans EmailService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RapportHebdomadaireService {

    private final ParentRepository parentRepository;
    private final TableauDeBordService tableauDeBordService;
    private final EmailService emailService;

    /**
     * Cron : chaque lundi a 08:00 (heure du serveur). Format cron Spring :
     * seconde minute heure jour-du-mois mois jour-de-la-semaine.
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void envoyerRapportsHebdomadaires() {
        log.info("Debut de l'envoi des rapports hebdomadaires parents");
        int envoyes = 0;

        for (Parent parent : parentRepository.findAll()) {
            if (parent.getEleves().isEmpty()) {
                continue;
            }
            try {
                String corps = construireCorpsRapport(parent);
                emailService.envoyerRapportHebdomadaire(parent.getEmail(), parent.getPrenom(), corps);
                envoyes++;
            } catch (Exception e) {
                log.error("Echec de generation/envoi du rapport hebdomadaire pour le parent {}", parent.getId(), e);
            }
        }

        log.info("Rapports hebdomadaires envoyes : {}", envoyes);
    }

    /** Expose separement pour permettre un declenchement manuel (tests, bouton admin, etc.). */
    public String construireCorpsRapport(Parent parent) {
        StringBuilder corps = new StringBuilder();
        corps.append("<p>Bonjour ").append(parent.getPrenom()).append(",</p>");
        corps.append("<p>Voici le resume de la semaine sur Xaam-Xaam+ :</p>");

        for (Eleve eleve : parent.getEleves()) {
            EleveSuiviResponse suivi = tableauDeBordService.construireSuiviEleve(eleve.getId());
            corps.append("<h3>").append(eleve.getPrenom()).append(" ").append(eleve.getNom()).append("</h3>");
            corps.append("<ul>");
            corps.append("<li>Exercices termines : ").append(suivi.nombreExercicesTermines()).append("</li>");
            corps.append("<li>Exercices en cours : ").append(suivi.nombreExercicesEnCours()).append("</li>");
            corps.append("<li>Tentatives au total : ").append(suivi.nombreTentativesTotal()).append("</li>");
            corps.append("<li>Score moyen de reformulation : ")
                    .append(String.format("%.0f", suivi.scoreMoyenReformulation())).append("/100</li>");
            corps.append("<li>Temps de reflexion moyen : ")
                    .append(String.format("%.0f", suivi.tempsReflexionMoyenMinutes())).append(" minutes</li>");
            corps.append("</ul>");
        }

        corps.append("<p>A la semaine prochaine !<br/>L'equipe Xaam-Xaam+</p>");
        return corps.toString();
    }
}
