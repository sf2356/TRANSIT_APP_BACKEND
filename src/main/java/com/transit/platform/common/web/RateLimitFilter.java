package com.transit.platform.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transit.platform.common.ApiErrorResponse;
import com.transit.platform.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prompt 04 §47 : scaffold de rate limiting sur les endpoints d'authentification les plus
 * sensibles aux abus (brute force, credential stuffing). Implémentation VOLONTAIREMENT
 * sans dépendance externe (pas de Bucket4j/Redis) : fenêtre fixe en mémoire, par IP, par
 * endpoint. Justification de ce choix (Prompt 04 demande de justifier toute librairie
 * ajoutée) : ajouter Bucket4j+Redis pour la V1 introduirait une dépendance d'infrastructure
 * (Redis) non prévue par l'architecture du Prompt 01, pour un besoin que ce scaffold
 * couvre suffisamment en mono-instance.
 *
 * LIMITE ASSUMÉE : ce compteur est local à l'instance JVM. En déploiement multi-instance
 * (plusieurs pods derrière un load balancer), chaque instance a sa propre limite — la
 * protection globale est donc plus faible que le chiffre configuré. À migrer vers un
 * backend partagé (Redis) si/quand une architecture multi-instance est mise en place.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> ENDPOINTS_PROTEGES = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh");
    private static final int MAX_REQUETES = 10;
    private static final long FENETRE_MS = 60_000;

    private final Map<String, Compteur> compteurs = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!ENDPOINTS_PROTEGES.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = path + "|" + clientIp(request);
        Compteur compteur = compteurs.computeIfAbsent(clientKey, k -> new Compteur());

        if (compteur.incrementerEtVerifier()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiErrorResponse body = ApiErrorResponse.of(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "Trop de tentatives — veuillez réessayer dans une minute", path);
            response.getWriter().write(objectMapper.writeValueAsString(body));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    /** Fenêtre fixe simple : suffisant pour dissuader un abus grossier, pas une protection cryptographique. */
    private static class Compteur {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().toEpochMilli();

        synchronized boolean incrementerEtVerifier() {
            long now = Instant.now().toEpochMilli();
            if (now - windowStart > FENETRE_MS) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= MAX_REQUETES;
        }
    }
}
