package com.transit.platform.validation;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.validation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/validations")
@Tag(name = "Validations", description = "Validations internes (dossier, facture, cotation, paiement, charge)")
public class ValidationRequestController {

    private final ValidationRequestService validationRequestService;

    public ValidationRequestController(ValidationRequestService validationRequestService) {
        this.validationRequestService = validationRequestService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VALIDATION_CREATE') or hasAuthority('VALIDATION_DECIDE')")
    public PagedApiResponse<ValidationResponse> search(@RequestParam(required = false) String statut,
                                                         @RequestParam(required = false) String entiteType,
                                                         @RequestParam(required = false) UUID demandeurId,
                                                         Pageable pageable) {
        return PagedApiResponse.of(validationRequestService.search(statut, entiteType, demandeurId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VALIDATION_CREATE') or hasAuthority('VALIDATION_DECIDE')")
    public ApiResponse<ValidationResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(validationRequestService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VALIDATION_CREATE')")
    @Operation(summary = "Demander une validation sur un dossier, une facture, une cotation, un paiement ou une charge")
    public ApiResponse<ValidationResponse> create(@Valid @RequestBody CreateValidationRequest request) {
        return ApiResponse.of(validationRequestService.create(request));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('VALIDATION_DECIDE')")
    public ApiResponse<ValidationResponse> approve(@PathVariable UUID id, @RequestBody(required = false) DecisionValidationRequest request) {
        return ApiResponse.of(validationRequestService.approve(id, request != null ? request : new DecisionValidationRequest(null)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('VALIDATION_DECIDE')")
    public ApiResponse<ValidationResponse> reject(@PathVariable UUID id, @RequestBody(required = false) DecisionValidationRequest request) {
        return ApiResponse.of(validationRequestService.reject(id, request != null ? request : new DecisionValidationRequest(null)));
    }
}
