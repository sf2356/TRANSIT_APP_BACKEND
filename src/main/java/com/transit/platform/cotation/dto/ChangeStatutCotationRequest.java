package com.transit.platform.cotation.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatutCotationRequest(@NotBlank String statut) {}
