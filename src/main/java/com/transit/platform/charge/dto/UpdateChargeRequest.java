package com.transit.platform.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateChargeRequest(
        UUID fournisseurId,
        @NotBlank String libelle,
        @NotBlank String type,
        String categorie,
        @NotNull @Positive BigDecimal montant,
        String devise,
        LocalDate dateCharge,
        String reference,
        String notes
) {}
