package com.transit.platform.tiers;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.tiers.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tiers")
@Tag(name = "Tiers", description = "Clients, fournisseurs et partenaires")
public class TiersController {

    private final TiersService tiersService;
    private final TiersHistoriqueService tiersHistoriqueService;

    public TiersController(TiersService tiersService, TiersHistoriqueService tiersHistoriqueService) {
        this.tiersService = tiersService;
        this.tiersHistoriqueService = tiersHistoriqueService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TIERS_READ')")
    public PagedApiResponse<TiersResponse> search(@RequestParam(required = false) String type,
                                                   @RequestParam(required = false) String statut,
                                                   @RequestParam(required = false) String search,
                                                   Pageable pageable) {
        return PagedApiResponse.of(tiersService.search(type, statut, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TIERS_READ')")
    public ApiResponse<TiersResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(tiersService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TIERS_CREATE')")
    public ApiResponse<TiersResponse> create(@Valid @RequestBody CreateTiersRequest request) {
        return ApiResponse.of(tiersService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TIERS_UPDATE')")
    public ApiResponse<TiersResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateTiersRequest request) {
        return ApiResponse.of(tiersService.update(id, request));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('TIERS_UPDATE')")
    public ApiResponse<Void> suspend(@PathVariable UUID id) {
        tiersService.suspend(id);
        return ApiResponse.of(null);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('TIERS_UPDATE')")
    public ApiResponse<Void> activate(@PathVariable UUID id) {
        tiersService.activate(id);
        return ApiResponse.of(null);
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAuthority('TIERS_READ')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Historique complet du client : dossiers, cotations, factures, paiements, marchandises, documents")
    public com.transit.platform.common.ApiResponse<com.transit.platform.tiers.dto.TiersHistoriqueResponse> historique(@PathVariable java.util.UUID id) {
        return com.transit.platform.common.ApiResponse.of(tiersHistoriqueService.getHistorique(id));
    }
}

