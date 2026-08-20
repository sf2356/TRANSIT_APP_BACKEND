package com.transit.platform.dossier.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * CONTRAT MIS À JOUR au Prompt 04 §15/§65 : structure imbriquée reflétant les étapes du
 * stepper (Prompt 01 §10 — Informations / Ordre de transit / Douane / Trajet / Instructions).
 * Remplace la structure plate du Prompt 03 (breaking change assumé et documenté au README —
 * à valider ensemble à l'exécution). Toujours pas de champ "numero" : généré par le backend.
 */
public record CreateDossierRequest(
        @NotNull(message = "Le client est obligatoire") UUID clientId,
        @NotBlank(message = "Le titre est obligatoire") String titre,
        String modeTransport,
        String priorite,
        UUID responsableId,
        LocalDate dateEcheance,
        @Valid OrdreTransitRequest ordreTransit,
        @Valid DouaneRequest douane,
        @Valid TrajetRequest trajet,
        String instructions,
        String description,
        String notes
) {}
