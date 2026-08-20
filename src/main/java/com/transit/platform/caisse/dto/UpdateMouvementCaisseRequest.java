package com.transit.platform.caisse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateMouvementCaisseRequest(
        String categorie,
        @NotBlank String libelle,
        @NotNull @Positive BigDecimal montant,
        String reference,
        String notes
) {}
