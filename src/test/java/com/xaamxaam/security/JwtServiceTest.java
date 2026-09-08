package com.xaamxaam.security;

import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "test-secret-key-with-at-least-256-bits-for-hmac-sha-signing";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600000L); // 1h
    }

    private Eleve unEleve(String email) {
        return Eleve.builder()
                .id(UUID.randomUUID())
                .nom("Diop")
                .prenom("Awa")
                .email(email)
                .motDePasse("hash")
                .role(Role.ELEVE)
                .actif(true)
                .niveauScolaire(Eleve.NiveauScolaire.TROISIEME)
                .build();
    }

    @Test
    void genererToken_produitUnTokenNonVideEtExtractibleEmail() {
        Eleve eleve = unEleve("awa@exemple.com");

        String token = jwtService.genererToken(eleve, Map.of("role", "ELEVE"));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraireEmail(token)).isEqualTo("awa@exemple.com");
    }

    @Test
    void estValide_retourneVraiPourLeBonUtilisateur() {
        Eleve eleve = unEleve("awa@exemple.com");
        String token = jwtService.genererToken(eleve, Map.of("role", "ELEVE"));

        assertThat(jwtService.estValide(token, eleve)).isTrue();
    }

    @Test
    void estValide_retourneFauxPourUnAutreUtilisateur() {
        Eleve eleve = unEleve("awa@exemple.com");
        Eleve autreEleve = unEleve("fatou@exemple.com");

        String token = jwtService.genererToken(eleve, Map.of("role", "ELEVE"));

        assertThat(jwtService.estValide(token, autreEleve)).isFalse();
    }

    @Test
    void tokenExpire_estRejeteParEstValide() throws InterruptedException {
        JwtService jwtServiceCourteExpiration = new JwtService(SECRET, 1L); // 1 ms
        Eleve eleve = unEleve("awa@exemple.com");

        String token = jwtServiceCourteExpiration.genererToken(eleve, Map.of("role", "ELEVE"));
        Thread.sleep(20);

        assertThat(jwtServiceCourteExpiration.estValide(token, eleve)).isFalse();
    }
}
