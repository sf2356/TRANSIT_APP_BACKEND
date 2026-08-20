package com.transit.platform.facture;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.facture.dto.CreateFactureRequest;
import com.transit.platform.facture.dto.FactureResponse;
import com.transit.platform.facture.dto.FactureSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoint explicitement demandé au Prompt 03 §21 : "Permettre de créer une facture
 * directement dans un dossier." Client, devise et dossier sont déduits automatiquement.
 */
@RestController
@RequestMapping("/api/v1/dossiers/{dossierId}/factures")
@Tag(name = "Factures", description = "Factures d'un dossier — création contextuelle (Prompt 03 §21)")
public class DossierFactureController {

    private final FactureService factureService;

    public DossierFactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FACTURE_READ')")
    public PagedApiResponse<FactureSummaryResponse> list(@PathVariable UUID dossierId, Pageable pageable) {
        return PagedApiResponse.of(factureService.listByDossier(dossierId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FACTURE_CREATE')")
    @Operation(summary = "Créer une facture depuis le dossier — client et devise déduits automatiquement, dossier jamais ressaisi")
    public ApiResponse<FactureResponse> create(@PathVariable UUID dossierId, @Valid @RequestBody CreateFactureRequest request) {
        return ApiResponse.of(factureService.createForDossier(dossierId, request));
    }
}
