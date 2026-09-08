package com.xaamxaam.service.auth;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.*;
import com.xaamxaam.dto.request.LoginRequest;
import com.xaamxaam.dto.request.RegisterRequest;
import com.xaamxaam.dto.response.AuthResponse;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EtablissementRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import com.xaamxaam.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementRepository etablissementRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public AuthResponse inscrire(RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Un compte existe deja avec cet email.");
        }

        Utilisateur utilisateur = construireUtilisateur(request);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));

        // Phase de lancement : validation manuelle par le superadmin, sauf pour
        // les eleves/parents dont le compte peut etre active directement
        // (le controle porte surtout sur les etablissements et enseignants).
        boolean activationImmediate = request.role() == Role.ELEVE || request.role() == Role.PARENT;
        utilisateur.setActif(activationImmediate);

        Utilisateur sauvegarde = utilisateurRepository.save(utilisateur);

        // L'envoi d'email ne bloque jamais l'inscription (voir EmailService :
        // toute erreur SMTP est loguee, jamais remontee ici).
        emailVerificationService.envoyerEmailDeVerification(sauvegarde);

        String token = jwtService.genererToken(sauvegarde, Map.of("role", sauvegarde.getRole().name()));
        return versReponse(token, sauvegarde);
    }

    public AuthResponse connecter(LoginRequest request) {
        loginAttemptService.verifierNonBloque(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.motDePasse())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.enregistrerEchec(request.email());
            throw e;
        }

        loginAttemptService.reinitialiserApresSucces(request.email());

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        utilisateur.setDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        String token = jwtService.genererToken(utilisateur, Map.of("role", utilisateur.getRole().name()));
        return versReponse(token, utilisateur);
    }

    private AuthResponse versReponse(String token, Utilisateur utilisateur) {
        return new AuthResponse(
                token, utilisateur.getEmail(), utilisateur.getNom(), utilisateur.getPrenom(),
                utilisateur.getRole(), utilisateur.isEmailVerifie()
        );
    }

    private Utilisateur construireUtilisateur(RegisterRequest request) {
        return switch (request.role()) {
            case ELEVE -> Eleve.builder()
                    .nom(request.nom())
                    .prenom(request.prenom())
                    .email(request.email())
                    .role(Role.ELEVE)
                    .niveauScolaire(Eleve.NiveauScolaire.depuisTexte(request.niveauScolaire()))
                    .etablissement(resoudreEtablissement(request.etablissementId()))
                    .build();
            case PARENT -> Parent.builder()
                    .nom(request.nom())
                    .prenom(request.prenom())
                    .email(request.email())
                    .role(Role.PARENT)
                    .build();
            case ENSEIGNANT -> Enseignant.builder()
                    .nom(request.nom())
                    .prenom(request.prenom())
                    .email(request.email())
                    .role(Role.ENSEIGNANT)
                    .matiere(request.matiere())
                    .etablissement(resoudreEtablissement(request.etablissementId()))
                    .build();
            case ADMIN_ETABLISSEMENT -> AdminEtablissement.builder()
                    .nom(request.nom())
                    .prenom(request.prenom())
                    .email(request.email())
                    .role(Role.ADMIN_ETABLISSEMENT)
                    .etablissement(resoudreEtablissement(request.etablissementId()))
                    .build();
            case SUPERADMIN -> Superadmin.builder()
                    .nom(request.nom())
                    .prenom(request.prenom())
                    .email(request.email())
                    .role(Role.SUPERADMIN)
                    .build();
        };
    }

    private Etablissement resoudreEtablissement(String etablissementId) {
        if (etablissementId == null || etablissementId.isBlank()) {
            return null;
        }
        return etablissementRepository.findById(UUID.fromString(etablissementId))
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement introuvable : " + etablissementId));
    }
}
