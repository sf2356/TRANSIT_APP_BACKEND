package com.transit.platform.dashboard.dto;

import java.math.BigDecimal;

public record DashboardFinanceResponse(
        BigDecimal totalFacture, BigDecimal totalCharges, BigDecimal totalEncaisse,
        BigDecimal soldeCaisse, BigDecimal resultatOperationnel
) {}
