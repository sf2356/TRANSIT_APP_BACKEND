package com.transit.platform.cotation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CotationResponse(
        UUID id, String numero, UUID clientId, UUID dossierId, String titre, LocalDate dateCotation,
        LocalDate dateValidite, String devise, String statut, BigDecimal montantHT, BigDecimal montantTaxe,
        BigDecimal montantTotal, String notes, String conditions, List<LigneCotationResponse> lignes
) {}
