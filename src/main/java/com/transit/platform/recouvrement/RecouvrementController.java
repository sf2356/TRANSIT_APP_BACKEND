package com.transit.platform.recouvrement;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.recouvrement.dto.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recouvrement")
@Tag(name = "Recouvrement", description = "Relances clients — email/WhatsApp/SMS prévus pour une itération ultérieure (Prompt 01 §19)")
public class RecouvrementController {

    private final RelanceService relanceService;

    public RecouvrementController(RelanceService relanceService) {
        this.relanceService = relanceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RECOUVREMENT_READ')")
    public PagedApiResponse<RelanceResponse> search(@RequestParam(required = false) UUID clientId,
                                                      @RequestParam(required = false) String statut,
                                                      @RequestParam(required = false) String statutFacture,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate echeanceAvant,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prochaineRelanceAvant,
                                                      Pageable pageable) {
        return PagedApiResponse.of(relanceService.search(clientId, statut, statutFacture, echeanceAvant, prochaineRelanceAvant, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RECOUVREMENT_READ')")
    public ApiResponse<RelanceResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(relanceService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RECOUVREMENT_CREATE')")
    public ApiResponse<RelanceResponse> create(@Valid @RequestBody CreateRelanceRequest request) {
        return ApiResponse.of(relanceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RECOUVREMENT_CREATE')")
    public ApiResponse<RelanceResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRelanceRequest request) {
        return ApiResponse.of(relanceService.update(id, request));
    }
}
