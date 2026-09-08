package com.xaamxaam.service.exercice;

import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.SignalementContournement;
import com.xaamxaam.domain.exercice.Tentative;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.repository.SignalementContournementRepository;
import com.xaamxaam.repository.TentativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detection heuristique de contournement (cf. section 4.2 - V2 : "detection
 * de contournement : copier-coller massif, changement de comportement
 * suspect"). Implementation volontairement simple pour le MVP : des regles
 * basees sur la longueur/vitesse/similitude, PAS de detection IA dediee.
 *
 * Principe important : cette detection ne bloque JAMAIS l'eleve. Elle
 * genere uniquement un signalement consultable par le superadmin, pour
 * eviter les faux positifs qui penaliseraient a tort un eleve qui redige
 * juste une reponse detaillee.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContournementDetectionService {

    private final SignalementContournementRepository signalementRepository;
    private final TentativeRepository tentativeRepository;

    private static final int SEUIL_LONGUEUR_SUSPECTE = 800;
    private static final long SEUIL_RAPIDITE_SUSPECTE_SECONDES = 3;

    @Transactional
    public void analyserTentative(Eleve eleve, Exercice exercice, Tentative tentative) {
        detecterCopieEnonce(eleve, exercice, tentative);
        detecterReponseAnormalementLongue(eleve, exercice, tentative);
        detecterRapiditeSuspecte(eleve, exercice, tentative);
    }

    private void detecterCopieEnonce(Eleve eleve, Exercice exercice, Tentative tentative) {
        String contenuNormalise = normaliser(tentative.getContenu());
        String enonceNormalise = normaliser(exercice.getEnonce());

        if (!contenuNormalise.isBlank() && contenuNormalise.equals(enonceNormalise)) {
            enregistrerSignalement(eleve, exercice,
                    SignalementContournement.TypeSignalement.COPIE_ENONCE,
                    "La tentative reproduit exactement l'enonce de l'exercice.");
        }
    }

    private void detecterReponseAnormalementLongue(Eleve eleve, Exercice exercice, Tentative tentative) {
        if (tentative.getContenu() != null && tentative.getContenu().length() > SEUIL_LONGUEUR_SUSPECTE) {
            enregistrerSignalement(eleve, exercice,
                    SignalementContournement.TypeSignalement.REPONSE_ANORMALEMENT_LONGUE,
                    "Tentative de " + tentative.getContenu().length() + " caracteres, potentiellement un copier-coller massif.");
        }
    }

    private void detecterRapiditeSuspecte(Eleve eleve, Exercice exercice, Tentative tentativeActuelle) {
        List<Tentative> historique = tentativeRepository.findByExerciceIdOrderByDateHeureAsc(exercice.getId());
        if (historique.size() < 2) {
            return;
        }

        // La tentative actuelle est deja persistee au moment de l'appel ; on
        // compare donc les deux dernieres entrees de l'historique.
        Tentative avantDerniere = historique.get(historique.size() - 2);
        Tentative derniere = historique.get(historique.size() - 1);

        if (avantDerniere.getDateHeure() == null || derniere.getDateHeure() == null) {
            return;
        }

        long ecartSecondes = Duration.between(avantDerniere.getDateHeure(), derniere.getDateHeure()).getSeconds();
        if (ecartSecondes >= 0 && ecartSecondes < SEUIL_RAPIDITE_SUSPECTE_SECONDES) {
            enregistrerSignalement(eleve, exercice,
                    SignalementContournement.TypeSignalement.RAPIDITE_SUSPECTE,
                    "Deux tentatives soumises en " + ecartSecondes + " secondes, potentiellement automatise.");
        }
    }

    private void enregistrerSignalement(Eleve eleve, Exercice exercice,
                                         SignalementContournement.TypeSignalement type, String details) {
        SignalementContournement signalement = SignalementContournement.builder()
                .eleve(eleve)
                .exercice(exercice)
                .type(type)
                .details(details)
                .build();
        signalementRepository.save(signalement);
        log.info("Signalement de contournement ({}) enregistre pour l'eleve {} sur l'exercice {}", type, eleve.getId(), exercice.getId());
    }

    private String normaliser(String texte) {
        return texte == null ? "" : texte.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
