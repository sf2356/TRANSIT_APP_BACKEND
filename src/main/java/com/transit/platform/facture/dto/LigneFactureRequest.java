package com.transit.platform.facture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Pas de champ montant/montantTaxe : toujours calculés par le backend (Prompt 03 §17, §25, §34). */
public record LigneFactureRequest(
        String categorieFrais,
        @NotBlank String description,
        @Positive BigDecimal quantite,
        @PositiveOrZero BigDecimal prixUnitaire,
        @PositiveOrZero BigDecimal tauxTaxe
) {}
