package com.transit.platform.auth.dto;

import java.util.UUID;

/** Vue minimale de l'entreprise embarquée dans /auth/me — évite un second appel GET /entreprise juste après login. */
public record EntrepriseResumeDto(UUID id, String nom, String devise, String logo, Long joursRestantsEssai) {}
