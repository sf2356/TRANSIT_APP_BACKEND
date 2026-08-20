package com.transit.platform.marchandise.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatutMarchandiseRequest(@NotBlank String statut) {}
