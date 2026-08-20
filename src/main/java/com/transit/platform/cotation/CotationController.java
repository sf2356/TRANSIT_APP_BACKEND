package com.transit.platform.cotation;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.cotation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cotations")
@Tag(name = "Cotations", description = "Cotations / devis — montants toujours calculés côté backend")
public class CotationController {

    private final CotationService cotationService;
    private final CotationPdfService cotationPdfService;

    public CotationController(CotationService cotationService, CotationPdfService cotationPdfService) {
        this.cotationService = cotationService;
        this.cotationPdfService = cotationPdfService;
    }

    @PostMapping("/{id}/facturer")
    @PreAuthorize("hasAuthority('FACTURE_CREATE')")
    public com.transit.platform.common.ApiResponse<com.transit.platform.facture.dto.FactureResponse> facturer(@PathVariable UUID id) {
        return com.transit.platform.common.ApiResponse.of(cotationService.facturer(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COTATION_READ')")
    public PagedApiResponse<CotationSummaryResponse> search(@RequestParam(required = false) String statut,
                                                              @RequestParam(required = false) UUID clientId,
                                                              @RequestParam(required = false) UUID dossierId,
                                                              @RequestParam(required = false) String search,
                                                              Pageable pageable) {
        return PagedApiResponse.of(cotationService.search(statut, clientId, dossierId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COTATION_READ')")
    public ApiResponse<CotationResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(cotationService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COTATION_CREATE')")
    @Operation(summary = "Créer une cotation autonome (hors dossier) — numéro COT-XXXX généré automatiquement")
    public ApiResponse<CotationResponse> create(@Valid @RequestBody CreateCotationRequest request) {
        return ApiResponse.of(cotationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COTATION_UPDATE')")
    public ApiResponse<CotationResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateCotationRequest request) {
        return ApiResponse.of(cotationService.update(id, request));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('COTATION_UPDATE')")
    public ApiResponse<CotationResponse> changeStatut(@PathVariable UUID id, @Valid @RequestBody ChangeStatutCotationRequest request) {
        return ApiResponse.of(cotationService.changeStatut(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COTATION_UPDATE')")
    @Operation(summary = "Supprimer une cotation (uniquement au statut BROUILLON)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        cotationService.delete(id);
        return ApiResponse.of(null);
    }

    @PostMapping("/{id}/lignes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COTATION_UPDATE')")
    @Operation(summary = "Ajouter une ligne — montant et montantTaxe recalculés par le backend")
    public ApiResponse<CotationResponse> addLigne(@PathVariable UUID id, @Valid @RequestBody LigneCotationRequest request) {
        return ApiResponse.of(cotationService.addLigne(id, request));
    }

    @PutMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('COTATION_UPDATE')")
    public ApiResponse<CotationResponse> updateLigne(@PathVariable UUID id, @PathVariable UUID ligneId,
                                                       @Valid @RequestBody LigneCotationRequest request) {
        return ApiResponse.of(cotationService.updateLigne(id, ligneId, request));
    }

    @DeleteMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('COTATION_UPDATE')")
    public ApiResponse<CotationResponse> deleteLigne(@PathVariable UUID id, @PathVariable UUID ligneId) {
        return ApiResponse.of(cotationService.deleteLigne(id, ligneId));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('COTATION_READ')")
    @Operation(summary = "Générer le PDF de la cotation (logo, coordonnées et signature de l'entreprise inclus)")
    public org.springframework.http.ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] pdf = cotationPdfService.genererPdf(id);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cotation.pdf\"")
                .body(pdf);
    }
}
