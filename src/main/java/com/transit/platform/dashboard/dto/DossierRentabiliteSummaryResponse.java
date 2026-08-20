package com.transit.platform.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DossierRentabiliteSummaryResponse(UUID dossierId, String numero, String titre, BigDecimal margeEstimee) {}
