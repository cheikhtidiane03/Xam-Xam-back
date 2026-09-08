package com.xaamxaam.service.auth;

import com.xaamxaam.domain.securite.TokenRevoque;
import com.xaamxaam.repository.TokenRevoqueRepository;
import com.xaamxaam.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Revocation explicite de tokens JWT (logout). Le JWT reste par nature
 * stateless jusqu'a son expiration naturelle ; cette liste noire en base
 * comble ce trou de securite pour le cas "l'utilisateur clique sur
 * deconnexion" ou "un token a fuite et doit etre invalide immediatement".
 *
 * Alternative plus scalable a grande echelle : un cache partage (Redis)
 * avec expiration automatique plutot qu'une table SQL nettoyee par job -
 * suffisant pour le volume attendu au lancement (cf. section 7 du cahier
 * des charges).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRevocationService {

    private final TokenRevoqueRepository tokenRevoqueRepository;
    private final JwtService jwtService;

    @Transactional
    public void revoquer(String token) {
        String jti = jwtService.extraireJti(token);
        if (jti == null || tokenRevoqueRepository.existsByJti(jti)) {
            return;
        }

        LocalDateTime expiration = jwtService.extraireExpiration(token).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        tokenRevoqueRepository.save(TokenRevoque.builder().jti(jti).dateExpiration(expiration).build());
    }

    public boolean estRevoque(String jti) {
        return jti != null && tokenRevoqueRepository.existsByJti(jti);
    }

    /** Nettoyage quotidien : purge les entrees dont le token est de toute facon expire. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgerTokensExpires() {
        tokenRevoqueRepository.deleteByDateExpirationBefore(LocalDateTime.now());
        log.debug("Purge des tokens revoques expires effectuee");
    }
}
