package com.xaamxaam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Generation et validation des tokens JWT utilises pour authentifier
 * les requetes apres connexion (voir AuthController / JwtAuthFilter).
 *
 * Chaque token porte un identifiant unique (claim "jti") qui permet de le
 * revoquer individuellement via TokenRevocationService, sans avoir a
 * attendre son expiration naturelle (voir logout dans AuthController).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String genererToken(UserDetails userDetails, Map<String, Object> claimsSupplementaires) {
        Map<String, Object> claims = new HashMap<>(claimsSupplementaires);
        claims.put("jti", UUID.randomUUID().toString());

        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraireEmail(String token) {
        return extraireClaim(token, Claims::getSubject);
    }

    public String extraireJti(String token) {
        return extraireClaim(token, claims -> claims.get("jti", String.class));
    }

    public Date extraireExpiration(String token) {
        return extraireClaim(token, Claims::getExpiration);
    }

    /**
     * Note importante : jjwt leve ExpiredJwtException directement au moment
     * du parsing d'un token expire (parseSignedClaims), plutot que de
     * retourner normalement des claims "expires". Cette methode capture
     * explicitement ce cas pour que estValide() reste un simple booleen,
     * sans faire remonter d'exception jusqu'au filtre HTTP.
     */
    public boolean estValide(String token, UserDetails userDetails) {
        try {
            String email = extraireEmail(token);
            return email.equals(userDetails.getUsername()) && !estExpire(token);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    private boolean estExpire(String token) {
        return extraireClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraireClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
