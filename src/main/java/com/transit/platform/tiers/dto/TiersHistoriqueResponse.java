package com.transit.platform.tiers.dto;

import com.transit.platform.cotation.dto.CotationSummaryResponse;
import com.transit.platform.dossier.dto.DossierSummaryResponse;
import com.transit.platform.facture.dto.FactureSummaryResponse;
import com.transit.platform.paiement.dto.PaiementResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Historique complet d'un client (demande utilisateur) : toutes les opérations passées,
 * tous modules confondus, en un seul appel — évite à Angular de faire 6 requêtes séparées
 * et de recomposer lui-même des données qui n'existent nulle part ailleurs sous cette forme
 * (marchandises/documents n'ont pas de lien direct vers le client, uniquement via le dossier).
 */
public record TiersHistoriqueResponse(
        List<DossierSummaryResponse> dossiers,
        List<CotationSummaryResponse> cotations,
        List<FactureSummaryResponse> factures,
        List<PaiementResponse> paiements,
        List<MarchandiseHistoriqueItem> marchandises,
        List<DocumentHistoriqueItem> documents
) {
    public record MarchandiseHistoriqueItem(
            UUID id, UUID dossierId, String dossierNumero, String designation, String statut, Instant dateAjout
    ) {}

    public record DocumentHistoriqueItem(
            UUID id, UUID dossierId, String dossierNumero, String titre, String typeDocument, Instant dateAjout
    ) {}
}