package com.transit.platform.cotation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneCotationResponse(
        UUID id, String categorieFrais, String description, BigDecimal quantite,
        BigDecimal prixUnitaire, BigDecimal montant, BigDecimal tauxTaxe, BigDecimal montantTaxe
) {}
