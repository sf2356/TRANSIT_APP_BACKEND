package com.transit.platform.caisse.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MouvementCaisseResponse(
        UUID id, UUID dossierId, UUID paiementId, String typeMouvement, String categorie, String libelle,
        BigDecimal montant, String devise, String modePaiement, Instant dateMouvement, String reference, String statut
) {}
