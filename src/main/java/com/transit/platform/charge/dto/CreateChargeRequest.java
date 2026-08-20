package com.transit.platform.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateChargeRequest(
        UUID fournisseurId,
        @NotBlank(message = "Le libellé est obligatoire") String libelle,
        @NotBlank(message = "Le type de charge est obligatoire") String type,
        String categorie,
        @NotNull @Positive(message = "Le montant doit être positif") BigDecimal montant,
        String devise,
        LocalDate dateCharge,
        String reference,
        String notes
) {}
