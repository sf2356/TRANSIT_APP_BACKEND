package com.transit.platform.entreprise;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.entreprise.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Namespace /api/v1/parametres explicitement demandé au Prompt 04 §31. Délègue entièrement
 * à EntrepriseService (aucune duplication de logique) — /api/v1/entreprise reste disponible
 * pour la compatibilité avec les intégrations issues du Prompt 03, les deux routes pointant
 * vers les mêmes données. Décision à confirmer : garder les deux namespaces en V1, ou
 * déprécier /api/v1/entreprise au profit de /api/v1/parametres/entreprise (voir README).
 */
@RestController
@RequestMapping("/api/v1/parametres")
@Tag(name = "Paramètres", description = "Paramètres de l'entreprise, numérotation, signature")
public class ParametresController {

    private final EntrepriseService entrepriseService;

    public ParametresController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @GetMapping
    @Operation(summary = "Paramètres généraux (devise, configuration métier)")
    public ApiResponse<ParametreEntrepriseResponse> get() {
        return ApiResponse.of(entrepriseService.getParametres());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    public ApiResponse<ParametreEntrepriseResponse> update(@Valid @RequestBody UpdateParametreEntrepriseRequest request) {
        return ApiResponse.of(entrepriseService.updateParametres(request));
    }

    @GetMapping("/entreprise")
    @Operation(summary = "Informations de l'entreprise (alias de GET /api/v1/entreprise)")
    public ApiResponse<EntrepriseResponse> getEntreprise() {
        return ApiResponse.of(entrepriseService.getCurrent());
    }

    @PutMapping("/entreprise")
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    public ApiResponse<EntrepriseResponse> updateEntreprise(@Valid @RequestBody UpdateEntrepriseRequest request) {
        return ApiResponse.of(entrepriseService.update(request));
    }

    /** Upload du logo — endpoint dédié car un logo est un fichier binaire, pas un champ texte du formulaire général. */
    @PostMapping(value = "/entreprise/logo", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    public ApiResponse<EntrepriseResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of(entrepriseService.uploadLogo(file));
    }

    @PostMapping(value = "/entreprise/cachet", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    public ApiResponse<EntrepriseResponse> uploadCachet(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of(entrepriseService.uploadCachet(file));
    }
    @GetMapping("/entreprise/logo")
    public ResponseEntity<?> telechargerLogo() {
        return repondreImage(entrepriseService.telechargerLogo());
    }

    @GetMapping("/entreprise/cachet")
    public ResponseEntity<?> telechargerCachet() {
        return repondreImage(entrepriseService.telechargerCachet());
    }

    private ResponseEntity<?> repondreImage(com.transit.platform.document.DocumentService.DownloadHandle handle) {
        if (handle.isRedirect()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(java.net.URI.create(handle.redirectUrl())).build();
        }
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(handle.contentType()))
                .body(new org.springframework.core.io.InputStreamResource(handle.stream()));
    }

    @GetMapping("/numerotation")
    @Operation(summary = "Préfixes de numérotation automatique (DOS/COT/FAC/PAY)")
    public ApiResponse<NumerotationResponse> getNumerotation() {
        return ApiResponse.of(entrepriseService.getNumerotation());
    }

    @PutMapping("/numerotation")
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    public ApiResponse<NumerotationResponse> updateNumerotation(@Valid @RequestBody UpdateNumerotationRequest request) {
        return ApiResponse.of(entrepriseService.updateNumerotation(request));
    }

    @GetMapping("/signature")
    @Operation(summary = "Signature interne utilisée sur les PDF (non certifiée en V1, cf. Prompt 02 §27)")
    public ApiResponse<SignatureResponse> getSignature() {
        return ApiResponse.of(entrepriseService.getSignature());
    }

    @PutMapping("/signature")
    @PreAuthorize("hasAuthority('PARAMETRE_UPDATE')")
    public ApiResponse<SignatureResponse> updateSignature(@Valid @RequestBody UpdateSignatureRequest request) {
        return ApiResponse.of(entrepriseService.updateSignature(request));
    }
}