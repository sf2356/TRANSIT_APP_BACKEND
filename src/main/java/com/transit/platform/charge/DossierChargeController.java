package com.transit.platform.charge;

import com.transit.platform.charge.dto.ChargeResponse;
import com.transit.platform.charge.dto.CreateChargeRequest;
import com.transit.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Endpoint contextualisé (Prompt 01 §9) : "Ajouter une charge" depuis la fiche dossier. */
@RestController
@RequestMapping("/api/v1/dossiers/{dossierId}/charges")
@Tag(name = "Charges", description = "Charges d'un dossier — création contextuelle")
public class DossierChargeController {

    private final ChargeService chargeService;

    public DossierChargeController(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CHARGE_CREATE')")
    @Operation(summary = "Ajouter une charge au dossier (dossier déduit de l'URL)")
    public ApiResponse<ChargeResponse> create(@PathVariable UUID dossierId, @Valid @RequestBody CreateChargeRequest request) {
        return ApiResponse.of(chargeService.createForDossier(dossierId, request));
    }
}
