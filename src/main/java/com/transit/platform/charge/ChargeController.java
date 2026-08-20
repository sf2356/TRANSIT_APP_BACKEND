package com.transit.platform.charge;

import com.transit.platform.charge.dto.ChargeResponse;
import com.transit.platform.charge.dto.UpdateChargeRequest;
import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/charges")
@Tag(name = "Charges", description = "Débours et charges rattachés à un dossier")
public class ChargeController {

    private final ChargeService chargeService;

    public ChargeController(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CHARGE_READ')")
    public PagedApiResponse<ChargeResponse> search(@RequestParam(required = false) UUID dossierId,
                                                     @RequestParam(required = false) String categorie,
                                                     @RequestParam(required = false) UUID fournisseurId,
                                                     @RequestParam(required = false) String statut,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                                                     Pageable pageable) {
        return PagedApiResponse.of(chargeService.search(dossierId, categorie, fournisseurId, statut, dateDebut, dateFin, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CHARGE_READ')")
    public ApiResponse<ChargeResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(chargeService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CHARGE_CREATE')")
    public ApiResponse<ChargeResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateChargeRequest request) {
        return ApiResponse.of(chargeService.update(id, request));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAuthority('CHARGE_CREATE')")
    public ApiResponse<Void> annuler(@PathVariable UUID id) {
        chargeService.annuler(id);
        return ApiResponse.of(null);
    }
}
