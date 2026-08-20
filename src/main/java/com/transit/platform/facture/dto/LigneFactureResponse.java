package com.transit.platform.facture.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneFactureResponse(
        UUID id, String categorieFrais, String description, BigDecimal quantite,
        BigDecimal prixUnitaire, BigDecimal montant, BigDecimal tauxTaxe, BigDecimal montantTaxe
) {}
