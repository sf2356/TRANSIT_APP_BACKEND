package com.transit.platform.paiement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaiementResponse(
        UUID id, String numero, UUID factureId, UUID cotationId, UUID dossierId, String dossierNumero, UUID clientId,
        BigDecimal montant, String devise, String modePaiement, LocalDate datePaiement, String reference,
        String statut, String observations
) {}
