package com.transit.platform.recouvrement.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RelanceResponse(
        UUID id, UUID factureId, UUID clientId, String typeRelance, String statut,
        LocalDate dateRelance, LocalDate prochaineRelance, String commentaire
) {}
