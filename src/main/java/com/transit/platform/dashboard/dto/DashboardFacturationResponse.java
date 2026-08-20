package com.transit.platform.dashboard.dto;

import java.math.BigDecimal;

public record DashboardFacturationResponse(
        BigDecimal totalFacture, BigDecimal totalEncaisse, BigDecimal resteAEncaisser,
        BigDecimal tauxEncaissement, long facturesEnRetard, BigDecimal montantEnRetard
) {}
