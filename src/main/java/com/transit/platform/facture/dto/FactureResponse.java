package com.transit.platform.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FactureResponse(
        UUID id, String numero, String typeDocument, UUID clientId, UUID dossierId, UUID cotationId, String titre,
        LocalDate dateDocument, LocalDate dateEcheance, String devise, String statut, BigDecimal montantHT,
        BigDecimal montantTaxe, BigDecimal montantTotal, BigDecimal montantPaye, BigDecimal resteAPayer,
        String notes, String conditions, List<LigneFactureResponse> lignes
) {}
