package com.transit.platform.facture;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.facture.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/factures")
@Tag(name = "Factures", description = "Factures / proformas — montants calculés côté backend, statut de paiement piloté par PaiementService")
public class FactureController {

    private final FactureService factureService;
    private final FacturePdfService facturePdfService;

    public FactureController(FactureService factureService, FacturePdfService facturePdfService) {
        this.factureService = factureService;
        this.facturePdfService = facturePdfService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FACTURE_READ')")
    public PagedApiResponse<FactureSummaryResponse> search(@RequestParam(required = false) String statut,
                                                             @RequestParam(required = false) UUID clientId,
                                                             @RequestParam(required = false) UUID dossierId,
                                                             @RequestParam(required = false) String search,
                                                             Pageable pageable) {
        return PagedApiResponse.of(factureService.search(statut, clientId, dossierId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURE_READ')")
    public ApiResponse<FactureResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(factureService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FACTURE_CREATE')")
    @Operation(summary = "Créer une facture autonome — numéro FAC-XXXX généré automatiquement")
    public ApiResponse<FactureResponse> create(@Valid @RequestBody CreateFactureRequest request) {
        return ApiResponse.of(factureService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURE_UPDATE')")
    public ApiResponse<FactureResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateFactureRequest request) {
        return ApiResponse.of(factureService.update(id, request));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('FACTURE_VALIDATE')")
    @Operation(summary = "Changer le statut manuel (BROUILLON/EMISE/ANNULEE) — PAYEE/PARTIELLEMENT_PAYEE sont calculés par les paiements")
    public ApiResponse<FactureResponse> changeStatut(@PathVariable UUID id, @Valid @RequestBody ChangeStatutFactureRequest request) {
        return ApiResponse.of(factureService.changeStatut(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURE_UPDATE')")
    @Operation(summary = "Supprimer une facture (uniquement BROUILLON et sans paiement enregistré)")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        factureService.delete(id);
        return ApiResponse.of(null);
    }

    @PostMapping("/{id}/lignes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FACTURE_UPDATE')")
    public ApiResponse<FactureResponse> addLigne(@PathVariable UUID id, @Valid @RequestBody LigneFactureRequest request) {
        return ApiResponse.of(factureService.addLigne(id, request));
    }

    @PutMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('FACTURE_UPDATE')")
    public ApiResponse<FactureResponse> updateLigne(@PathVariable UUID id, @PathVariable UUID ligneId,
                                                      @Valid @RequestBody LigneFactureRequest request) {
        return ApiResponse.of(factureService.updateLigne(id, ligneId, request));
    }

    @DeleteMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('FACTURE_UPDATE')")
    public ApiResponse<FactureResponse> deleteLigne(@PathVariable UUID id, @PathVariable UUID ligneId) {
        return ApiResponse.of(factureService.deleteLigne(id, ligneId));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('FACTURE_READ')")
    @Operation(summary = "Générer le PDF de la facture (logo, coordonnées et signature de l'entreprise inclus)")
    public org.springframework.http.ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] pdf = facturePdfService.genererPdf(id);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"facture.pdf\"")
                .body(pdf);
    }
}
