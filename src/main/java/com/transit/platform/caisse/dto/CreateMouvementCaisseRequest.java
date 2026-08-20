package com.transit.platform.caisse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateMouvementCaisseRequest(
        UUID dossierId,
        @NotBlank(message = "Le type de mouvement est obligatoire (ENTREE ou SORTIE)") String typeMouvement,
        String categorie,
        @NotBlank String libelle,
        @NotNull @Positive(message = "Le montant doit être positif") BigDecimal montant,
        String devise,
        String modePaiement,
        String reference,
        String notes
) {}
