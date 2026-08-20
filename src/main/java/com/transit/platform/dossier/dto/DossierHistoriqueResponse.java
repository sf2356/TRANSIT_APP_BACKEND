package com.transit.platform.dossier.dto;

import java.time.Instant;
import java.util.UUID;

public record DossierHistoriqueResponse(UUID id, String evenement, String description, UUID utilisateurId, Instant dateEvenement) {}
