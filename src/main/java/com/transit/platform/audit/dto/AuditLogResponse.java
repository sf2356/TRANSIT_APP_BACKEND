package com.transit.platform.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id, UUID utilisateurId, String action, String entiteType, UUID entiteId,
        Map<String, Object> ancienneValeur, Map<String, Object> nouvelleValeur, String adresseIp, Instant dateAction
) {}
