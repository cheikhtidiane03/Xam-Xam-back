package com.xaamxaam.service.auth;

import com.xaamxaam.domain.conformite.TokenVerificationEmail;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.exception.TokenInvalideException;
import com.xaamxaam.repository.TokenVerificationEmailRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import com.xaamxaam.service.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Verification d'email a l'inscription. Note de conception (voir README) :
 * pour le MVP, le compte reste utilisable (login possible) meme sans email
 * verifie - bloquer le login des le depart risquerait de casser la demo
 * si le SMTP n'est pas configure a temps. Le flag emailVerifie est
 * neanmoins expose partout (AuthResponse) pour que le frontend puisse
 * afficher un bandeau "verifie ton email" et, plus tard, restreindre
 * certaines actions si necessaire.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UtilisateurRepository utilisateurRepository;
    private final TokenVerificationEmailRepository tokenRepository;
    private final EmailService emailService;

    private static final long EXPIRATION_HEURES = 24;

    @Transactional
    public void envoyerEmailDeVerification(Utilisateur utilisateur) {
        String token = UUID.randomUUID().toString();
        TokenVerificationEmail entite = TokenVerificationEmail.builder()
                .utilisateur(utilisateur)
                .token(token)
                .dateExpiration(LocalDateTime.now().plusHours(EXPIRATION_HEURES))
                .build();
        tokenRepository.save(entite);
        emailService.envoyerVerificationEmail(utilisateur.getEmail(), utilisateur.getPrenom(), token);
    }

    @Transactional
    public void verifier(String token) {
        TokenVerificationEmail entite = tokenRepository.findByTokenAndUtiliseFalse(token)
                .orElseThrow(() -> new TokenInvalideException("Token de verification invalide ou deja utilise."));

        if (entite.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new TokenInvalideException("Ce lien de verification a expire. Demande un nouvel envoi.");
        }

        Utilisateur utilisateur = entite.getUtilisateur();
        utilisateur.setEmailVerifie(true);
        utilisateurRepository.save(utilisateur);

        entite.setUtilise(true);
        tokenRepository.save(entite);
    }
}
