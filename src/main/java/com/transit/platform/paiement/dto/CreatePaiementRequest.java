package com.transit.platform.paiement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Au moins un parmi factureId/cotationId/dossierId doit être fourni (Prompt 02 §16 règle
 * applicative, reflétée par la contrainte CHECK en base). Lorsque factureId est fourni,
 * client et dossier sont déduits automatiquement (Prompt 03 §19) — clientId/dossierId ne
 * sont alors pas nécessaires dans la requête.
 */
public record CreatePaiementRequest(
        UUID factureId,
        UUID cotationId,
        UUID dossierId,
        UUID clientId,
        @NotNull @Positive(message = "Le montant doit être positif") BigDecimal montant,
        String devise,
        @NotBlank(message = "Le mode de paiement est obligatoire") String modePaiement,
        LocalDate datePaiement,
        String reference,
        String observations
) {}
