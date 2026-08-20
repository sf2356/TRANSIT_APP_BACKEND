package com.transit.platform.document.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DocumentResponse(
        UUID id, UUID dossierId, String dossierNumero, UUID marchandiseId, UUID factureId, UUID cotationId,
        String titre, String typeDocument, String nomFichier, String typeMime, long taille,
        String statut, LocalDate dateReception, LocalDate dateExpiration, UUID ajoutePar, Instant dateAjout
) {}
