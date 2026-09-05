package com.transit.platform.cotation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CotationSummaryResponse(
        UUID id, String numero, UUID clientId, UUID dossierId, String dossierNumero, String statut,
        BigDecimal montantTotal, String devise, LocalDate dateCotation
) {}
