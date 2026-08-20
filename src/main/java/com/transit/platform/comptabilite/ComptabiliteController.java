package com.transit.platform.comptabilite;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.comptabilite.dto.ComptabiliteOperationnelleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comptabilite")
@Tag(name = "Comptabilité opérationnelle", description = "Pré-comptabilité de consolidation — ne remplace pas un logiciel comptable complet (Prompt 03 §33)")
public class ComptabiliteController {

    private final ComptabiliteService comptabiliteService;

    public ComptabiliteController(ComptabiliteService comptabiliteService) {
        this.comptabiliteService = comptabiliteService;
    }

    @GetMapping("/operationnelle")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    @Operation(summary = "Consolidation facturé/encaissé/charges/caisse, filtrable par période, client, dossier")
    public ApiResponse<ComptabiliteOperationnelleResponse> operationnelle(@RequestParam(required = false) UUID clientId,
                                                                            @RequestParam(required = false) UUID dossierId,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ApiResponse.of(comptabiliteService.operationnelle(clientId, dossierId, dateDebut, dateFin));
    }
}
