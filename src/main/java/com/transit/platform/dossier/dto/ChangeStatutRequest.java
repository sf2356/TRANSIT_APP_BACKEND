package com.transit.platform.dossier.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatutRequest(@NotBlank String statut) {}
