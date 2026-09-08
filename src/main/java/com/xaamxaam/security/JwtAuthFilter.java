package com.xaamxaam.security;

import com.xaamxaam.service.auth.TokenRevocationService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre execute une fois par requete : extrait le JWT de l'en-tete
 * Authorization, verifie qu'il n'a pas ete explicitement revoque (logout),
 * le valide, et peuple le SecurityContext si tout est en ordre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String jti = jwtService.extraireJti(token);
            if (tokenRevocationService.estRevoque(jti)) {
                log.debug("Token revoque (logout) rejete, jti={}", jti);
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extraireEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.estValide(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            // Token malforme, expire ou signature invalide : on n'authentifie
            // simplement pas la requete (elle sera rejetee en 401/403 plus
            // loin dans la chaine si l'endpoint necessite une authentification).
            log.debug("Token JWT invalide recu : {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
