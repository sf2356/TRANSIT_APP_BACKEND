package com.transit.platform.cotation;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.cotation.dto.CotationResponse;
import com.transit.platform.cotation.dto.CotationSummaryResponse;
import com.transit.platform.cotation.dto.CreateCotationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Endpoint contextualisé (Prompt 01 §9) : client déduit automatiquement du dossier. */
@RestController
@RequestMapping("/api/v1/dossiers/{dossierId}/cotations")
@Tag(name = "Cotations", description = "Cotations d'un dossier — création contextuelle")
public class DossierCotationController {

    private final CotationService cotationService;

    public DossierCotationController(CotationService cotationService) {
        this.cotationService = cotationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COTATION_READ')")
    public PagedApiResponse<CotationSummaryResponse> list(@PathVariable UUID dossierId, Pageable pageable) {
        return PagedApiResponse.of(cotationService.listByDossier(dossierId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COTATION_CREATE')")
    @Operation(summary = "Créer une cotation depuis le dossier — client et devise déduits automatiquement")
    public ApiResponse<CotationResponse> create(@PathVariable UUID dossierId, @Valid @RequestBody CreateCotationRequest request) {
        return ApiResponse.of(cotationService.createForDossier(dossierId, request));
    }
}
