package com.transit.platform.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateValidationRequest(
        @NotBlank(message = "Le type de validation est obligatoire") String type,
        @NotBlank(message = "Le type d'entité est obligatoire (DOSSIER, FACTURE, COTATION, PAIEMENT, CHARGE)") String entiteType,
        @NotNull(message = "L'identifiant de l'entité concernée est obligatoire") UUID entiteId,
        String commentaire
) {}
