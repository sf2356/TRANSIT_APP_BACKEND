package com.transit.platform.entreprise;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.entreprise.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CORRECTIF AUDIT (Prompt 07 §58) : namespace redondant avec /api/v1/parametres/entreprise
 * (Prompt 04 §31), vérifié comme n'ayant AUCUN consommateur réel côté Angular ni Flutter
 * (les deux appellent exclusivement /parametres/entreprise). Conservé plutôt que supprimé
 * par prudence (Prompt 07 §57 : ne pas casser un endpoint sans certitude absolue sur ses
 * consommateurs — un usage externe non couvert par cet audit reste possible), mais
 * explicitement marqué déprécié pour orienter toute nouvelle intégration vers la bonne route.
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/entreprise")
@Tag(name = "Entreprise", description = "DÉPRÉCIÉ — utiliser /api/v1/parametres/entreprise. Conservé pour compatibilité descendante (Prompt 07 §58).")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @GetMapping
    @Operation(summary = "Consulter les informations de l'entreprise courante")
    public ApiResponse<EntrepriseResponse> get() {
        return ApiResponse.of(entrepriseService.getCurrent());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    @Operation(summary = "Modifier les informations de l'entreprise courante")
    public ApiResponse<EntrepriseResponse> update(@Valid @RequestBody UpdateEntrepriseRequest request) {
        return ApiResponse.of(entrepriseService.update(request));
    }

    @GetMapping("/parametres")
    @Operation(summary = "Consulter les paramètres (préfixes de numérotation, signature, devise)")
    public ApiResponse<ParametreEntrepriseResponse> getParametres() {
        return ApiResponse.of(entrepriseService.getParametres());
    }

    @PutMapping("/parametres")
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    @Operation(summary = "Modifier les paramètres de l'entreprise")
    public ApiResponse<ParametreEntrepriseResponse> updateParametres(@Valid @RequestBody UpdateParametreEntrepriseRequest request) {
        return ApiResponse.of(entrepriseService.updateParametres(request));
    }
}
