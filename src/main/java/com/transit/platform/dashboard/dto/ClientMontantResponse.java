package com.transit.platform.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ClientMontantResponse(UUID clientId, String raisonSociale, BigDecimal totalFacture) {}
