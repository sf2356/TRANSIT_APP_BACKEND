package com.transit.platform.dashboard;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.dashboard.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Tableaux de bord — toutes les données sont calculées côté backend")
@PreAuthorize("hasAuthority('DASHBOARD_READ')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/global")
    @Operation(summary = "Vue d'ensemble : dossiers, facturation, encaissements, résultat opérationnel")
    public ApiResponse<DashboardGlobalResponse> global() {
        return ApiResponse.of(dashboardService.global());
    }

    @GetMapping("/direction")
    @Operation(summary = "Vue direction : top clients, dossiers rentables, dossiers à risque")
    public ApiResponse<DashboardDirectionResponse> direction() {
        return ApiResponse.of(dashboardService.direction());
    }

    @GetMapping("/operations")
    @Operation(summary = "Vue opérationnelle : dossiers par statut, échéances proches")
    public ApiResponse<DashboardOperationsResponse> operations() {
        return ApiResponse.of(dashboardService.operations());
    }

    @GetMapping("/facturation")
    @Operation(summary = "Vue facturation : facturé, encaissé, reste à encaisser, taux d'encaissement")
    public ApiResponse<DashboardFacturationResponse> facturation() {
        return ApiResponse.of(dashboardService.facturation());
    }

    @GetMapping("/recouvrement")
    @Operation(summary = "Vue recouvrement : factures en retard, relances par statut")
    public ApiResponse<DashboardRecouvrementResponse> recouvrement() {
        return ApiResponse.of(dashboardService.recouvrement());
    }

    @GetMapping("/finance")
    @Operation(summary = "Vue finance : facturé, charges, encaissé, solde de caisse, résultat opérationnel")
    public ApiResponse<DashboardFinanceResponse> finance() {
        return ApiResponse.of(dashboardService.finance());
    }
}
