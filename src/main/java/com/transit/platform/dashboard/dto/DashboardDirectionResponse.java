package com.transit.platform.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDirectionResponse(
        long nombreDossiers, BigDecimal totalFacture, BigDecimal totalEncaisse, BigDecimal resultatOperationnel,
        BigDecimal tauxEncaissement, List<ClientMontantResponse> topClients,
        List<DossierRentabiliteSummaryResponse> dossiersRentables, List<DossierRentabiliteSummaryResponse> dossiersARisque
) {}
