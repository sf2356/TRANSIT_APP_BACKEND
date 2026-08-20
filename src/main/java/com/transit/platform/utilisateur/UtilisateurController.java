package com.transit.platform.utilisateur;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.utilisateur.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utilisateurs")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs de l'entreprise")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @Operation(summary = "Lister les utilisateurs (pagination, recherche)")
    public PagedApiResponse<UtilisateurResponse> search(@RequestParam(required = false) String search, Pageable pageable) {
        return PagedApiResponse.of(utilisateurService.search(search, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<UtilisateurResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(utilisateurService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('UTILISATEUR_CREATE')")
    @Operation(summary = "Créer un utilisateur et lui affecter un ou plusieurs rôles")
    public ApiResponse<UtilisateurResponse> create(@Valid @RequestBody CreateUtilisateurRequest request) {
        return ApiResponse.of(utilisateurService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UTILISATEUR_UPDATE')")
    public ApiResponse<UtilisateurResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUtilisateurRequest request) {
        return ApiResponse.of(utilisateurService.update(id, request));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('UTILISATEUR_UPDATE')")
    @Operation(summary = "Suspendre un utilisateur (jamais de suppression physique : préserve l'historique/audit)")
    public ApiResponse<Void> suspend(@PathVariable UUID id) {
        utilisateurService.suspend(id);
        return ApiResponse.of(null);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('UTILISATEUR_UPDATE')")
    public ApiResponse<Void> restore(@PathVariable UUID id) {
        utilisateurService.restore(id);
        return ApiResponse.of(null);
    }
}
