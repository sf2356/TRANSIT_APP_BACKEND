package com.transit.platform.dossier.dto;

import java.math.BigDecimal;

/**
 * Reflète le format attendu au Prompt 03 §32. totalCharges regroupe l'ensemble des charges
 * du dossier ; droitsTaxes/transport/manutention sont des sous-totaux par TypeCharge, utiles
 * pour l'affichage détaillé sans recalcul côté client.
 */
public record DossierRentabiliteResponse(
        BigDecimal totalFacture,
        BigDecimal totalEncaisse,
        BigDecimal resteAEncaisser,
        BigDecimal totalCharges,
        BigDecimal droitsTaxes,
        BigDecimal transport,
        BigDecimal manutention,
        BigDecimal margeEstimee
) {}
