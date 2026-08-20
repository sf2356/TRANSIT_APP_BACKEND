package com.transit.platform.dossier.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Version allégée pour les listes — évite de transporter les champs longs (instructions, notes...). */
public record DossierSummaryResponse(
        UUID id, String numero, String titre, UUID clientId, String statut, String priorite,
        UUID responsableId, LocalDate dateOuverture, LocalDate dateEcheance
) {}
