package com.transit.platform.dashboard.dto;

import java.util.Map;

public record DashboardOperationsResponse(
        long nombreDossiers, Map<String, Long> dossiersParStatut, long dossiersProchesEcheance
) {}
