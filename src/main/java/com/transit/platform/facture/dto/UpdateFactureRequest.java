package com.transit.platform.facture.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateFactureRequest(
        UUID clientId,
        String titre,
        LocalDate dateEcheance,
        String devise,
        String notes,
        String conditions
) {}
