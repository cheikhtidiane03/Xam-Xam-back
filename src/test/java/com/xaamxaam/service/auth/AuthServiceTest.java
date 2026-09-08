package com.xaamxaam.service.auth;

import com.xaamxaam.domain.user.Role;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.dto.request.RegisterRequest;
import com.xaamxaam.repository.EtablissementRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import com.xaamxaam.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private EtablissementRepository etablissementRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private LoginAttemptService loginAttemptService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                utilisateurRepository, etablissementRepository, passwordEncoder,
                authenticationManager, jwtService, emailVerificationService, loginAttemptService
        );
    }

    @Test
    void inscrire_activeImmediatementUnCompteEleve() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.genererToken(any(), any())).thenReturn("token-jwt");

        RegisterRequest request = new RegisterRequest(
                "Diop", "Awa", "awa@exemple.com", "motdepasse123", Role.ELEVE, null, "TROISIEME", null
        );

        authService.inscrire(request);

        verify(utilisateurRepository).save(argThat(Utilisateur::isActif));
        verify(emailVerificationService).envoyerEmailDeVerification(any());
    }

    @Test
    void inscrire_activeImmediatementUnComptePparent() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.genererToken(any(), any())).thenReturn("token-jwt");

        RegisterRequest request = new RegisterRequest(
                "Ndiaye", "Fatou", "fatou@exemple.com", "motdepasse123", Role.PARENT, null, null, null
        );

        authService.inscrire(request);

        verify(utilisateurRepository).save(argThat(Utilisateur::isActif));
    }

    @Test
    void inscrire_laisseEnAttenteUnCompteSuperadmin() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.genererToken(any(), any())).thenReturn("token-jwt");

        RegisterRequest request = new RegisterRequest(
                "Ba", "Cheikh", "cheikh@exemple.com", "motdepasse123", Role.SUPERADMIN, null, null, null
        );

        authService.inscrire(request);

        verify(utilisateurRepository).save(argThat(u -> !u.isActif()));
    }

    @Test
    void inscrire_refuseSiLemailExisteDeja() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "Diop", "Awa", "awa@exemple.com", "motdepasse123", Role.ELEVE, null, "TROISIEME", null
        );

        assertThatThrownBy(() -> authService.inscrire(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(utilisateurRepository, never()).save(any());
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void inscrire_hacheLeMotDePasseAvantSauvegarde() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("$2a$hash-securise");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.genererToken(any(), any())).thenReturn("token-jwt");

        RegisterRequest request = new RegisterRequest(
                "Diop", "Awa", "awa@exemple.com", "motdepasse123", Role.ELEVE, null, "TROISIEME", null
        );

        authService.inscrire(request);

        verify(utilisateurRepository).save(argThat(u -> u.getPassword().equals("$2a$hash-securise")));
    }

    @Test
    void connecter_verifieLeBlocageBruteForceAvantAuthentification() {
        doThrow(new com.xaamxaam.exception.CompteTemporairementBloqueException("Bloque"))
                .when(loginAttemptService).verifierNonBloque(anyString());

        com.xaamxaam.dto.request.LoginRequest request =
                new com.xaamxaam.dto.request.LoginRequest("awa@exemple.com", "motdepasse123");

        assertThatThrownBy(() -> authService.connecter(request))
                .isInstanceOf(com.xaamxaam.exception.CompteTemporairementBloqueException.class);

        verifyNoInteractions(authenticationManager);
    }
}
