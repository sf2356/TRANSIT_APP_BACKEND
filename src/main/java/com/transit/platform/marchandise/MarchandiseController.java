package com.transit.platform.marchandise;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.marchandise.dto.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Endpoints "plats" (recherche transverse, accès direct par id) — la création reste contextualisée, voir DossierMarchandiseController. */
@RestController
@RequestMapping("/api/v1/marchandises")
@Tag(name = "Marchandises", description = "Marchandises rattachées aux dossiers")
public class MarchandiseController {

    private final MarchandiseService marchandiseService;

    public MarchandiseController(MarchandiseService marchandiseService) {
        this.marchandiseService = marchandiseService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MARCHANDISE_READ')")
    public PagedApiResponse<MarchandiseResponse> search(@RequestParam(required = false) String statut,
                                                          @RequestParam(required = false) String search,
                                                          Pageable pageable) {
        return PagedApiResponse.of(marchandiseService.searchGlobal(statut, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MARCHANDISE_READ')")
    public ApiResponse<MarchandiseResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(marchandiseService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MARCHANDISE_UPDATE')")
    public ApiResponse<MarchandiseResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateMarchandiseRequest request) {
        return ApiResponse.of(marchandiseService.update(id, request));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('MARCHANDISE_UPDATE')")
    public ApiResponse<MarchandiseResponse> changeStatut(@PathVariable UUID id, @Valid @RequestBody ChangeStatutMarchandiseRequest request) {
        return ApiResponse.of(marchandiseService.changeStatut(id, request));
    }
}
