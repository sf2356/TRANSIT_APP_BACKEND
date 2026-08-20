package com.transit.platform.security;

import com.transit.platform.utilisateur.Utilisateur;
import com.transit.platform.utilisateur.UtilisateurRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtre d'authentification JWT : lit le Bearer token, le valide, et reconstruit le
 * contexte de sécurité (utilisateur + entreprise + permissions) pour la requête.
 *
 * Vérifie systématiquement que l'utilisateur porté par le token existe toujours et
 * est actif — un token valide ne suffit pas si le compte a été suspendu entre-temps.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UtilisateurRepository utilisateurRepository) {
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7);
            Claims claims = jwtService.parseClaims(token);
            if (jwtService.isRefreshToken(claims)) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID utilisateurId = UUID.fromString(claims.get("uid", String.class));
            UUID entrepriseId = UUID.fromString(claims.get("eid", String.class));

            // Revalidation systématique en base : un token émis avant une suspension de compte
            // ne doit jamais rester utilisable jusqu'à son expiration naturelle.
            Utilisateur utilisateur = utilisateurRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(utilisateurId, entrepriseId)
                    .orElse(null);

            if (utilisateur != null && "ACTIF".equals(utilisateur.getStatut())) {
                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("perms", List.class);
                CurrentUserPrincipal principal = CurrentUserPrincipal.of(
                        utilisateurId, entrepriseId, claims.getSubject(), permissions);
                var authToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token JWT invalide ou expiré : {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
