package com.xaamxaam.service.auth;

import com.xaamxaam.config.SecuriteProperties;
import com.xaamxaam.domain.securite.TentativeConnexionEchouee;
import com.xaamxaam.exception.CompteTemporairementBloqueException;
import com.xaamxaam.repository.TentativeConnexionEchoueeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Protection brute-force basique sur /api/auth/login : au-dela d'un nombre
 * d'echecs configurable (app.securite.max-tentatives-connexion) sur une
 * fenetre glissante (app.securite.fenetre-blocage-minutes), les connexions
 * pour cet email sont temporairement bloquees.
 *
 * Implementation basee sur une table SQL (donc partagee si plusieurs
 * instances backend tournent derriere le meme PostgreSQL). Pour un volume
 * plus important, un cache partage (Redis) serait plus performant, mais
 * une table suffit largement au volume attendu au lancement.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final TentativeConnexionEchoueeRepository repository;
    private final SecuriteProperties securiteProperties;

    public void verifierNonBloque(String email) {
        long echecsRecents = compterEchecsRecents(email);
        if (echecsRecents >= securiteProperties.maxTentativesConnexion()) {
            throw new CompteTemporairementBloqueException(
                    "Trop de tentatives de connexion echouees. Reessaie dans "
                            + securiteProperties.fenetreBlocageMinutes() + " minutes, "
                            + "ou utilise 'mot de passe oublie'."
            );
        }
    }

    @Transactional
    public void enregistrerEchec(String email) {
        repository.save(TentativeConnexionEchouee.builder().email(email).build());
    }

    @Transactional
    public void reinitialiserApresSucces(String email) {
        // On ne supprime pas retroactivement les echecs precedents ligne par
        // ligne (couteux) : le job de purge quotidien s'en charge. Un succes
        // de connexion ne "debloque" pas immediatement un compte deja
        // bloque dans la fenetre courante - comportement volontairement
        // strict pour rester simple.
    }

    private long compterEchecsRecents(String email) {
        LocalDateTime depuis = LocalDateTime.now().minusMinutes(securiteProperties.fenetreBlocageMinutes());
        return repository.countByEmailAndDateTentativeAfter(email, depuis);
    }

    /** Purge quotidienne des tentatives anciennes pour ne pas faire grossir la table indefiniment. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgerAnciennesTentatives() {
        repository.deleteByDateTentativeBefore(LocalDateTime.now().minusDays(7));
    }
}
