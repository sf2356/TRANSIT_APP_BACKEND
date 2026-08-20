package com.transit.platform.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardRecouvrementResponse(
        long facturesEnRetard, BigDecimal montantEnRetard, Map<String, Long> relancesParStatut
) {}
