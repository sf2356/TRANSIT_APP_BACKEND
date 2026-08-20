package com.transit.platform.dossier.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DossierResponse(
        UUID id, String numero, UUID clientId, String titre, String modeTransport, String priorite,
        UUID responsableId, LocalDate dateOuverture, LocalDate dateEcheance, LocalDate dateCloture, String statut,
        String numeroOrdreTransit, LocalDate dateOrdreTransit, String referenceClient, String donneurOrdre,
        String typeOperation, String regimeDouanier, String incoterm, String origine, String provenance,
        String destination, String instructions, String description, String notes
) {}
