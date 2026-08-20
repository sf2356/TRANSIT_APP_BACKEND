package com.transit.platform.cotation.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateCotationRequest(
        UUID clientId,
        String titre,
        LocalDate dateValidite,
        String devise,
        String notes,
        String conditions
) {}
