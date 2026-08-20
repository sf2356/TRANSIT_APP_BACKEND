package com.transit.platform.cotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Pas de champ "numero" (généré par le backend). dossierId est optionnel : une cotation peut
 * être créée hors dossier (avant-vente), ou de façon contextualisée via
 * POST /dossiers/{dossierId}/cotations, auquel cas le dossier ET son client sont déduits.
 */
public record CreateCotationRequest(
        UUID clientId,
        String titre,
        LocalDate dateValidite,
        String devise,
        String notes,
        String conditions,
        @NotEmpty(message = "Une cotation doit contenir au moins une ligne") @Valid List<LigneCotationRequest> lignes
) {}
