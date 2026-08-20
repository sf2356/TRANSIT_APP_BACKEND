package com.transit.platform.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardGlobalResponse(
        long nombreDossiers, Map<String, Long> dossiersParStatut, long dossiersProchesEcheance,
        BigDecimal totalFacture, BigDecimal totalEncaisse, BigDecimal resteAEncaisser, BigDecimal totalCharges,
        BigDecimal resultatOperationnel, BigDecimal tauxEncaissement, long facturesEnRetard, BigDecimal montantEnRetard
) {}
