package com.xaamxaam.seed;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.repository.EtablissementRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Active automatiquement TOUS les comptes utilisateurs et etablissements
 * en attente, a CHAQUE demarrage de l'application. Purement une commodite
 * de developpement local : evite de devoir taper une requete SQL manuelle
 * (UPDATE utilisateurs SET actif = true ...) a chaque fois qu'un nouveau
 * compte est cree pendant les tests.
 *
 * N'est actif QUE sous le profil "dev" - jamais en production, ou ce
 * comportement contournerait completement le controle manuel des
 * inscriptions voulu en section 12 du cahier des charges.
 *
 * Contrairement a SeedDataRunner (qui CREE des donnees de test une seule
 * fois de facon idempotente), celui-ci ne cree rien : il se contente
 * d'activer ce qui existe deja et qui est encore en attente, a chaque
 * redemarrage - donc aucune action manuelle a refaire a chaque fois que
 * tu crees un nouveau compte via /api/auth/register.
 *
 * Activation : ajoute SPRING_PROFILES_ACTIVE=dev dans les variables
 * d'environnement de ta configuration IntelliJ (a faire une seule fois,
 * ca reste actif a chaque redemarrage ensuite).
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAutoActivationRunner implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementRepository etablissementRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Utilisateur> comptesInactifs = utilisateurRepository.findAll().stream()
                .filter(u -> !u.isActif())
                .toList();

        comptesInactifs.forEach(u -> {
            u.setActif(true);
            u.setEmailVerifie(true);
        });
        utilisateurRepository.saveAll(comptesInactifs);

        List<Etablissement> etablissementsEnAttente = etablissementRepository.findAll().stream()
                .filter(Etablissement::isEnAttenteValidation)
                .toList();

        etablissementsEnAttente.forEach(e -> {
            e.setEnAttenteValidation(false);
            e.setLicenceActive(true);
        });
        etablissementRepository.saveAll(etablissementsEnAttente);

        if (comptesInactifs.isEmpty() && etablissementsEnAttente.isEmpty()) {
            log.info("[dev] Aucun compte ni etablissement en attente a activer.");
        } else {
            log.info(
                    "[dev] Auto-activation : {} compte(s) et {} etablissement(s) actives automatiquement.",
                    comptesInactifs.size(), etablissementsEnAttente.size()
            );
            comptesInactifs.forEach(u -> log.info("[dev]   -> compte active : {} ({})", u.getEmail(), u.getRole()));
        }
    }
}
