package com.transit.platform.paiement;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.paiement.dto.PaiementFactureRequest;
import com.transit.platform.paiement.dto.PaiementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoint contextualisé (Prompt 01 §9, Prompt 03 §20) : "Créer un paiement à partir d'une
 * facture" déduit automatiquement client et dossier — voir GET /api/v1/factures/{id}/paiements
 * du Prompt 03 §25 pour la consultation.
 */
@RestController
@RequestMapping("/api/v1/factures/{factureId}/paiements")
@Tag(name = "Paiements", description = "Paiements d'une facture — création contextuelle")
public class FacturePaiementController {

    private final PaiementService paiementService;

    public FacturePaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAIEMENT_READ')")
    public PagedApiResponse<PaiementResponse> list(@PathVariable UUID factureId, Pageable pageable) {
        return PagedApiResponse.of(paiementService.listByFacture(factureId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PAIEMENT_CREATE')")
    @Operation(summary = "Enregistrer un paiement sur cette facture — client et dossier déduits automatiquement",
            description = "Supporte l'en-tête Idempotency-Key (Prompt 04 §41-42).")
    public ApiResponse<PaiementResponse> create(@PathVariable UUID factureId, @Valid @RequestBody PaiementFactureRequest request,
                                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.of(paiementService.createForFacture(factureId, request, idempotencyKey));
    }
}
