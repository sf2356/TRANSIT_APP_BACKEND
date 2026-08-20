package com.transit.platform.cotation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Pas de champ "montant" ni "montantTaxe" ici : ils sont TOUJOURS calculés par le backend
 * (quantite × prixUnitaire, puis application du tauxTaxe) — jamais acceptés du client
 * (Prompt 03 §24, §34).
 */
public record LigneCotationRequest(
        String categorieFrais,
        @NotBlank String description,
        @Positive(message = "La quantité doit être positive") BigDecimal quantite,
        @PositiveOrZero(message = "Le prix unitaire doit être positif ou nul") BigDecimal prixUnitaire,
        @PositiveOrZero BigDecimal tauxTaxe
) {}
