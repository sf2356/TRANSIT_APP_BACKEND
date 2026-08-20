package com.transit.platform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Prompt 04 §48 : logge méthode, endpoint, utilisateur, statut HTTP et durée pour CHAQUE
 * requête API — jamais de mot de passe, de JWT complet ou de donnée sensible.
 *
 * ORDRE D'EXÉCUTION (à VÉRIFIER ensemble à l'exécution, non testable dans cet environnement
 * sans serveur PostgreSQL/Maven) : @Order(LOWEST_PRECEDENCE) place ce filtre au plus près de
 * la servlet, donc APRÈS la chaîne interne de Spring Security (et donc après
 * JwtAuthenticationFilter) — c'est ce qui permet de lire l'utilisateur authentifié ici. Si
 * l'utilisateur remonte toujours "anonyme" dans les logs à l'exécution, c'est le premier
 * point à vérifier.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.transit.platform.api");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String utilisateur = currentUserEmail();
            log.info("{} {} -> {} ({} ms) [user={}]", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), durationMs, utilisateur);
        }
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() ? authentication.getName() : "anonyme";
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Évite de polluer les logs avec les probes de santé et la doc Swagger.
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }
}
