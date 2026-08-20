package com.transit.platform.dossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateDossierRequest(
        @NotNull UUID clientId,
        @NotBlank String titre,
        String modeTransport,
        String priorite,
        LocalDate dateEcheance,
        String numeroOrdreTransit,
        LocalDate dateOrdreTransit,
        String referenceClient,
        String donneurOrdre,
        String typeOperation,
        String regimeDouanier,
        String incoterm,
        String origine,
        String provenance,
        String destination,
        String instructions,
        String description,
        String notes
) {}
