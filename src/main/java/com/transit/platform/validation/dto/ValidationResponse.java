package com.transit.platform.validation.dto;

import java.time.Instant;
import java.util.UUID;

public record ValidationResponse(
        UUID id, String type, String entiteType, UUID entiteId, UUID demandeurId, UUID validateurId,
        String statut, String commentaire, Instant dateDemande, Instant dateDecision
) {}
