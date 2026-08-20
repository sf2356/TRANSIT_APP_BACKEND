package com.transit.platform.recouvrement;

import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.recouvrement.dto.RelanceResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Câble l'endpoint GET /factures/{id}/relances demandé au Prompt 03 §25, différé jusqu'à ce que Recouvrement existe. */
@RestController
@RequestMapping("/api/v1/factures/{factureId}/relances")
@Tag(name = "Recouvrement", description = "Historique des relances d'une facture")
public class FactureRelanceController {

    private final RelanceService relanceService;

    public FactureRelanceController(RelanceService relanceService) {
        this.relanceService = relanceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RECOUVREMENT_READ')")
    public PagedApiResponse<RelanceResponse> list(@PathVariable UUID factureId, Pageable pageable) {
        return PagedApiResponse.of(relanceService.listByFacture(factureId, pageable));
    }
}
