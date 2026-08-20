package com.transit.platform.caisse;

import com.transit.platform.caisse.dto.*;
import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/caisse")
@Tag(name = "Caisse", description = "Mouvements de caisse — solde toujours recalculé, jamais stocké")
public class CaisseController {

    private final CaisseService caisseService;

    public CaisseController(CaisseService caisseService) {
        this.caisseService = caisseService;
    }

    @GetMapping("/mouvements")
    @PreAuthorize("hasAuthority('CAISSE_READ')")
    public PagedApiResponse<MouvementCaisseResponse> search(@RequestParam(required = false) String typeMouvement,
                                                              @RequestParam(required = false) String statut,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateDebut,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFin,
                                                              Pageable pageable) {
        return PagedApiResponse.of(caisseService.search(typeMouvement, statut, dateDebut, dateFin, pageable));
    }

    @GetMapping("/mouvements/{id}")
    @PreAuthorize("hasAuthority('CAISSE_READ')")
    public ApiResponse<MouvementCaisseResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(caisseService.getById(id));
    }

    @PostMapping("/mouvements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CAISSE_CREATE')")
    @Operation(summary = "Enregistrer un mouvement de caisse manuel (hors paiement automatique)")
    public ApiResponse<MouvementCaisseResponse> create(@Valid @RequestBody CreateMouvementCaisseRequest request) {
        return ApiResponse.of(caisseService.create(request));
    }

    @PutMapping("/mouvements/{id}")
    @PreAuthorize("hasAuthority('CAISSE_CREATE')")
    public ApiResponse<MouvementCaisseResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateMouvementCaisseRequest request) {
        return ApiResponse.of(caisseService.update(id, request));
    }

    @PatchMapping("/mouvements/{id}/annuler")
    @PreAuthorize("hasAuthority('CAISSE_CREATE')")
    public ApiResponse<Void> annuler(@PathVariable UUID id) {
        caisseService.annuler(id);
        return ApiResponse.of(null);
    }

    @GetMapping("/resume")
    @PreAuthorize("hasAuthority('CAISSE_READ')")
    @Operation(summary = "Totaux entrées/sorties, solde calculé, mouvements en attente")
    public ApiResponse<CaisseResumeResponse> resume() {
        return ApiResponse.of(caisseService.resume());
    }
}
