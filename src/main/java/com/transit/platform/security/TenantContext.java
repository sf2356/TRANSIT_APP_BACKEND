package com.transit.platform.security;

import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Point d'accès UNIQUE au contexte tenant courant (utilisateur + entreprise authentifiés).
 *
 * Tous les services métier doivent passer par ce composant plutôt que de dupliquer la
 * lecture du SecurityContext. C'est la garantie centrale de l'isolation multi-tenant :
 * un seul endroit du code sait "quelle est l'entreprise courante", et il ne fait jamais
 * confiance à une valeur envoyée par le client.
 */
@Component
public class TenantContext {

    public CurrentUserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Utilisateur non authentifié", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }

    public UUID currentEntrepriseId() {
        return currentUser().getEntrepriseId();
    }

    public UUID currentUtilisateurId() {
        return currentUser().getUtilisateurId();
    }
}
