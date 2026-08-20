package com.transit.platform.paiement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Utilisé par l'endpoint contextualisé POST /factures/{id}/paiements : la facture (donc client et dossier) est déduite de l'URL. */
public record PaiementFactureRequest(
        @NotNull @Positive(message = "Le montant doit être positif") BigDecimal montant,
        @NotBlank(message = "Le mode de paiement est obligatoire") String modePaiement,
        LocalDate datePaiement,
        String reference,
        String observations
) {}
