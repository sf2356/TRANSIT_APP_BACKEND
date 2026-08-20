package com.transit.platform.security;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Principal Spring Security enrichi avec l'identité métier (utilisateurId, entrepriseId).
 *
 * IMPORTANT (règle multi-tenant non négociable) : l'entrepriseId ici provient UNIQUEMENT
 * du JWT signé par le backend lors du login, jamais d'un paramètre envoyé par le client.
 * Tout service métier doit résoudre l'entreprise courante via TenantContext, jamais via
 * un champ entrepriseId reçu dans un DTO de requête.
 */
public class CurrentUserPrincipal extends User {

    private final UUID utilisateurId;
    private final UUID entrepriseId;

    public CurrentUserPrincipal(UUID utilisateurId, UUID entrepriseId, String email,
                                 Collection<? extends GrantedAuthority> authorities) {
        super(email, "", authorities);
        this.utilisateurId = utilisateurId;
        this.entrepriseId = entrepriseId;
    }

    public static CurrentUserPrincipal of(UUID utilisateurId, UUID entrepriseId, String email,
                                           Collection<String> permissionCodes) {
        Collection<GrantedAuthority> authorities = permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
        return new CurrentUserPrincipal(utilisateurId, entrepriseId, email, authorities);
    }

    public UUID getUtilisateurId() {
        return utilisateurId;
    }

    public UUID getEntrepriseId() {
        return entrepriseId;
    }
}
