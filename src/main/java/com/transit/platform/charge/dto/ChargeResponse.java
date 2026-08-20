package com.transit.platform.charge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ChargeResponse(
        UUID id, UUID dossierId, UUID fournisseurId, String libelle, String type, String categorie,
        BigDecimal montant, String devise, String statut, LocalDate dateCharge, String reference, String notes
) {}
