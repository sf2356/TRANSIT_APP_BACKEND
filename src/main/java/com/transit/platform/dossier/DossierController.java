package com.transit.platform.dossier;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.dossier.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dossiers")
@Tag(name = "Dossiers", description = "Dossier de transit — entité centrale de la plateforme")
public class DossierController {

    private final DossierService dossierService;
    private final DossierRentabiliteService rentabiliteService;
    private final DossierResumeService resumeService;

    public DossierController(DossierService dossierService, DossierRentabiliteService rentabiliteService,
                              DossierResumeService resumeService) {
        this.dossierService = dossierService;
        this.rentabiliteService = rentabiliteService;
        this.resumeService = resumeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOSSIER_READ')")
    @Operation(summary = "Lister les dossiers (pagination, filtres statut/client/responsable, recherche)")
    public PagedApiResponse<DossierSummaryResponse> search(@RequestParam(required = false) String statut,
                                                             @RequestParam(required = false) UUID clientId,
                                                             @RequestParam(required = false) UUID responsableId,
                                                             @RequestParam(required = false) String search,
                                                             Pageable pageable) {
        return PagedApiResponse.of(dossierService.search(statut, clientId, responsableId, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOSSIER_READ')")
    public ApiResponse<DossierResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(dossierService.getById(id));
    }

    /**
     * Vue synthétique agrégée (Prompt 04 §16/§38) — remplace l'ancien alias qui renvoyait
     * simplement le dossier brut (Prompt 03). Contrat étendu volontairement : c'est
     * exactement le type de "correction de contrat" à valider ensemble à l'exécution.
     */
    @GetMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('DOSSIER_READ')")
    @Operation(summary = "Vue synthétique agrégée : dossier + compteurs + rentabilité, en un seul appel")
    public ApiResponse<com.transit.platform.dossier.dto.DossierResumeResponse> resume(@PathVariable UUID id) {
        return ApiResponse.of(resumeService.resume(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DOSSIER_CREATE')")
    @Operation(summary = "Créer un dossier — le numéro DOS-XXXX est généré automatiquement par le backend")
    public ApiResponse<DossierResponse> create(@Valid @RequestBody CreateDossierRequest request) {
        return ApiResponse.of(dossierService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOSSIER_UPDATE')")
    public ApiResponse<DossierResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateDossierRequest request) {
        return ApiResponse.of(dossierService.update(id, request));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('DOSSIER_UPDATE')")
    public ApiResponse<DossierResponse> changeStatut(@PathVariable UUID id, @Valid @RequestBody ChangeStatutRequest request) {
        return ApiResponse.of(dossierService.changeStatut(id, request));
    }

    @PatchMapping("/{id}/responsable")
    @PreAuthorize("hasAuthority('DOSSIER_UPDATE')")
    public ApiResponse<DossierResponse> changeResponsable(@PathVariable UUID id, @Valid @RequestBody ChangeResponsableRequest request) {
        return ApiResponse.of(dossierService.changeResponsable(id, request));
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasAuthority('DOSSIER_CLOSE')")
    @Operation(summary = "Clôturer un dossier (uniquement depuis le statut TERMINE)")
    public ApiResponse<DossierResponse> cloturer(@PathVariable UUID id) {
        return ApiResponse.of(dossierService.cloturer(id));
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAuthority('DOSSIER_READ')")
    @Operation(summary = "Timeline du dossier (création, changements de statut, documents ajoutés...)")
    public PagedApiResponse<DossierHistoriqueResponse> historique(@PathVariable UUID id, Pageable pageable) {
        return PagedApiResponse.of(dossierService.getHistorique(id, pageable));
    }

    @GetMapping("/{id}/rentabilite")
    @PreAuthorize("hasAuthority('DOSSIER_READ')")
    @Operation(summary = "Rentabilité indicative du dossier (facturé, encaissé, charges, marge estimée) — Prompt 03 §32")
    public ApiResponse<com.transit.platform.dossier.dto.DossierRentabiliteResponse> rentabilite(@PathVariable UUID id) {
        return ApiResponse.of(rentabiliteService.calculer(id));
    }
}
