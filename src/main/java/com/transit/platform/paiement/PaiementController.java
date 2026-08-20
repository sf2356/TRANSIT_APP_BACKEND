package com.transit.platform.paiement;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.paiement.dto.CreatePaiementRequest;
import com.transit.platform.paiement.dto.PaiementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/paiements")
@Tag(name = "Paiements", description = "Encaissements — recalcul automatique du statut de la facture liée")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAIEMENT_READ')")
    public PagedApiResponse<PaiementResponse> search(@RequestParam(required = false) UUID factureId,
                                                       @RequestParam(required = false) UUID dossierId,
                                                       @RequestParam(required = false) String statut,
                                                       Pageable pageable) {
        return PagedApiResponse.of(paiementService.search(factureId, dossierId, statut, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAIEMENT_READ')")
    public ApiResponse<PaiementResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(paiementService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PAIEMENT_CREATE')")
    @Operation(summary = "Enregistrer un paiement — numéro PAY-XXXX généré automatiquement, statut facture recalculé",
            description = "Supporte l'en-tête Idempotency-Key pour éviter un double enregistrement en cas de double clic (Prompt 04 §41-42).")
    public ApiResponse<PaiementResponse> create(@Valid @RequestBody CreatePaiementRequest request,
                                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.of(paiementService.create(request, idempotencyKey));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAuthority('PAIEMENT_CREATE')")
    @Operation(summary = "Annuler un paiement — recalcule automatiquement la facture liée")
    public ApiResponse<Void> annuler(@PathVariable UUID id) {
        paiementService.annuler(id);
        return ApiResponse.of(null);
    }
}
