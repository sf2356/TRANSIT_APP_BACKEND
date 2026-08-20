package com.transit.platform.marchandise;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.marchandise.dto.CreateMarchandiseRequest;
import com.transit.platform.marchandise.dto.MarchandiseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoint contextualisé (Prompt 01 §9 / Prompt 03 §22) : depuis la fiche dossier,
 * "Ajouter une marchandise" ne demande jamais de re-sélectionner le dossier.
 */
@RestController
@RequestMapping("/api/v1/dossiers/{dossierId}/marchandises")
@Tag(name = "Marchandises", description = "Marchandises d'un dossier — création contextuelle")
public class DossierMarchandiseController {

    private final MarchandiseService marchandiseService;

    public DossierMarchandiseController(MarchandiseService marchandiseService) {
        this.marchandiseService = marchandiseService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MARCHANDISE_READ')")
    public PagedApiResponse<MarchandiseResponse> list(@PathVariable UUID dossierId, Pageable pageable) {
        return PagedApiResponse.of(marchandiseService.listByDossier(dossierId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MARCHANDISE_CREATE')")
    @Operation(summary = "Ajouter une marchandise au dossier (dossier déduit de l'URL, jamais ressaisi)")
    public ApiResponse<MarchandiseResponse> create(@PathVariable UUID dossierId, @Valid @RequestBody CreateMarchandiseRequest request) {
        return ApiResponse.of(marchandiseService.createForDossier(dossierId, request));
    }
}
