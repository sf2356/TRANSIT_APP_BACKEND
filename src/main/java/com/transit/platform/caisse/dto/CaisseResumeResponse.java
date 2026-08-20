package com.transit.platform.caisse.dto;

import java.math.BigDecimal;

public record CaisseResumeResponse(
        BigDecimal totalEntrees, BigDecimal totalSorties, BigDecimal solde,
        long mouvementsEnAttente, long nombreMouvements
) {}
