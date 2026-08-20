package com.transit.platform.facture.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FactureSummaryResponse(
        UUID id, String numero, String typeDocument, UUID clientId, UUID dossierId, String statut,
        BigDecimal montantTotal, BigDecimal resteAPayer, String devise, LocalDate dateDocument, LocalDate dateEcheance
) {}
