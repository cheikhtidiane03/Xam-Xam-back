package com.xaamxaam.service.auth;

import com.xaamxaam.domain.conformite.TokenReinitialisationMdp;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.exception.TokenInvalideException;
import com.xaamxaam.repository.TokenReinitialisationMdpRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import com.xaamxaam.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flux "mot de passe oublie" : demande -> email avec token -> confirmation.
 * Par securite, demanderReinitialisation ne revele jamais si l'email existe
 * ou non en base (reponse identique dans les deux cas cote controller).
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UtilisateurRepository utilisateurRepository;
    private final TokenReinitialisationMdpRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final long EXPIRATION_MINUTES = 60;

    @Transactional
    public void demanderReinitialisation(String email) {
        utilisateurRepository.findByEmail(email).ifPresent(utilisateur -> {
            String token = UUID.randomUUID().toString();
            TokenReinitialisationMdp entite = TokenReinitialisationMdp.builder()
                    .utilisateur(utilisateur)
                    .token(token)
                    .dateExpiration(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                    .build();
            tokenRepository.save(entite);
            emailService.envoyerReinitialisationMotDePasse(utilisateur.getEmail(), utilisateur.getPrenom(), token);
        });
        // Aucune exception si l'email est inconnu : evite de reveler quels
        // emails sont enregistres sur la plateforme (enumeration attack).
    }

    @Transactional
    public void reinitialiser(String token, String nouveauMotDePasse) {
        TokenReinitialisationMdp entite = tokenRepository.findByTokenAndUtiliseFalse(token)
                .orElseThrow(() -> new TokenInvalideException("Token de reinitialisation invalide ou deja utilise."));

        if (entite.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new TokenInvalideException("Ce lien de reinitialisation a expire. Fais une nouvelle demande.");
        }

        Utilisateur utilisateur = entite.getUtilisateur();
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);

        entite.setUtilise(true);
        tokenRepository.save(entite);
    }
}
