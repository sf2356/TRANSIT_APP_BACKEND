package com.transit.platform.recouvrement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRelanceRequest(
        @NotNull(message = "La facture est obligatoire") UUID factureId,
        @NotBlank(message = "Le type de relance est obligatoire") String typeRelance,
        LocalDate dateRelance,
        LocalDate prochaineRelance,
        String commentaire
) {}
