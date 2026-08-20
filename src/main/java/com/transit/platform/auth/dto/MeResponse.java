package com.transit.platform.auth.dto;

import java.util.List;
import java.util.UUID;

/**
 * Format aligné sur Prompt 04 §10. Les permissions sont fournies uniquement pour l'AFFICHAGE
 * côté client (masquer/afficher des actions) — le backend revalide systématiquement chaque
 * permission sur chaque endpoint via @PreAuthorize, jamais de confiance dans l'UI.
 */
public record MeResponse(
        UUID id, String nom, String prenom, String email, String telephone,
        List<RoleResumeDto> roles, List<String> permissions,
        EntrepriseResumeDto entreprise, String villeAffectation, String statut
) {}
