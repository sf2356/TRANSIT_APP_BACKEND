package com.transit.platform.facture.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Pas de champ "numero" (généré par le backend). clientId est optionnel si la facture est
 * créée de façon contextualisée (POST /dossiers/{dossierId}/factures ou depuis une cotation) :
 * dans ce cas, client et devise sont déduits automatiquement (Prompt 03 §21).
 */
public record CreateFactureRequest(
        UUID clientId,
        String typeDocument,
        String titre,
        LocalDate dateEcheance,
        String devise,
        String notes,
        String conditions,
        @NotEmpty(message = "Une facture doit contenir au moins une ligne") @Valid List<LigneFactureRequest> lignes
) {}
