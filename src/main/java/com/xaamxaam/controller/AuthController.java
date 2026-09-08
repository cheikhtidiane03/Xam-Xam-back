package com.xaamxaam.controller;

import com.xaamxaam.dto.request.*;
import com.xaamxaam.dto.response.AuthResponse;
import com.xaamxaam.service.auth.AuthService;
import com.xaamxaam.service.auth.EmailVerificationService;
import com.xaamxaam.service.auth.PasswordResetService;
import com.xaamxaam.service.auth.TokenRevocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, mot de passe oublie, verification email")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final TokenRevocationService tokenRevocationService;

    @PostMapping("/register")
    @Operation(summary = "Creer un compte",
            description = "Cree un compte pour l'un des 5 roles. Les comptes Eleve/Parent sont actives "
                    + "immediatement ; les autres roles restent en attente de validation par le Superadmin "
                    + "pendant la phase de lancement. Un email de verification est envoye dans tous les cas.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.inscrire(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter",
            description = "Retourne un token JWT a utiliser dans l'en-tete Authorization: Bearer <token>. "
                    + "Bloque temporairement apres plusieurs echecs consecutifs (protection brute-force).")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.connecter(request));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Se deconnecter",
            description = "Revoque immediatement le token JWT courant (liste noire), au lieu d'attendre son expiration naturelle.")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replaceFirst("^Bearer ", "");
        tokenRevocationService.revoquer(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mot-de-passe-oublie")
    @Operation(summary = "Demander une reinitialisation de mot de passe",
            description = "Envoie un email avec un lien de reinitialisation si l'adresse correspond a un compte. "
                    + "Repond toujours 204, meme si l'email est inconnu (evite de reveler quels emails sont enregistres).")
    public ResponseEntity<Void> motDePasseOublie(@Valid @RequestBody MotDePasseOublieRequest request) {
        passwordResetService.demanderReinitialisation(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    @Operation(summary = "Confirmer la reinitialisation avec le token recu par email")
    public ResponseEntity<Void> reinitialiserMotDePasse(@Valid @RequestBody ReinitialiserMotDePasseRequest request) {
        passwordResetService.reinitialiser(request.token(), request.nouveauMotDePasse());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verifier-email")
    @Operation(summary = "Confirmer l'adresse email avec le token recu")
    public ResponseEntity<Void> verifierEmail(@Valid @RequestBody VerifierEmailRequest request) {
        emailVerificationService.verifier(request.token());
        return ResponseEntity.noContent().build();
    }
}
