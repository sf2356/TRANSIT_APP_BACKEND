package com.transit.platform.facture.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatutFactureRequest(@NotBlank String statut) {}
