package com.transit.platform.utilisateur.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UtilisateurResponse(
        UUID id, String nom, String prenom, String email, String telephone,
        String villeAffectation, String statut, Instant derniereConnexion, List<String> roles
) {}
