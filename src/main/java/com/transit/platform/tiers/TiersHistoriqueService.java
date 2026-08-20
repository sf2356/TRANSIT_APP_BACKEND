package com.transit.platform.tiers;

import com.transit.platform.cotation.CotationService;
import com.transit.platform.cotation.dto.CotationSummaryResponse;
import com.transit.platform.document.Document;
import com.transit.platform.document.DocumentRepository;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.dto.DossierSummaryResponse;
import com.transit.platform.facture.FactureService;
import com.transit.platform.facture.dto.FactureSummaryResponse;
import com.transit.platform.marchandise.Marchandise;
import com.transit.platform.marchandise.MarchandiseRepository;
import com.transit.platform.paiement.Paiement;
import com.transit.platform.paiement.PaiementRepository;
import com.transit.platform.paiement.dto.PaiementResponse;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.dto.TiersHistoriqueResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Historique complet d'un client (demande utilisateur), tous modules confondus, en un seul
 * appel. Service dédié — plutôt que d'alourdir TiersService — pour éviter tout risque de
 * dépendance circulaire avec les services métier qu'il orchestre (même précaution que pour
 * la transformation cotation \u2192 facture).
 */
@Service
public class TiersHistoriqueService {

    private final TiersService tiersService;
    private final DossierService dossierService;
    private final CotationService cotationService;
    private final FactureService factureService;
    private final PaiementRepository paiementRepository;
    private final MarchandiseRepository marchandiseRepository;
    private final DocumentRepository documentRepository;
    private final TenantContext tenantContext;

    public TiersHistoriqueService(TiersService tiersService, DossierService dossierService,
                                  CotationService cotationService, FactureService factureService,
                                  PaiementRepository paiementRepository, MarchandiseRepository marchandiseRepository,
                                  DocumentRepository documentRepository, TenantContext tenantContext) {
        this.tiersService = tiersService;
        this.dossierService = dossierService;
        this.cotationService = cotationService;
        this.factureService = factureService;
        this.paiementRepository = paiementRepository;
        this.marchandiseRepository = marchandiseRepository;
        this.documentRepository = documentRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public TiersHistoriqueResponse getHistorique(UUID clientId) {
        // Vérifie que le tiers existe bien dans le tenant courant avant de continuer
        // (lève TIERS_NOT_FOUND sinon — jamais d'historique d'un tiers d'une autre entreprise).
        tiersService.getById(clientId);

        UUID entrepriseId = tenantContext.currentEntrepriseId();

        List<DossierSummaryResponse> dossiers = dossierService.search(null, clientId, null, null, PageRequest.of(0, 100)).getContent();
        List<CotationSummaryResponse> cotations = cotationService.search(null, clientId, null, null, PageRequest.of(0, 100)).getContent();
        List<FactureSummaryResponse> factures = factureService.search(null, clientId, null, null, PageRequest.of(0, 100)).getContent();

        List<Paiement> paiements = paiementRepository.findByClientId(clientId, entrepriseId);
        List<Marchandise> marchandises = marchandiseRepository.findByClientId(clientId, entrepriseId);
        List<Document> documents = documentRepository.findByClientId(clientId, entrepriseId);

        // Table de correspondance dossierId -> numero, pour enrichir marchandises/documents
        // sans requête supplémentaire par ligne (elles n'ont pas de lien direct vers le client).
        Map<UUID, String> numeroParDossier = dossiers.stream()
                .collect(Collectors.toMap(DossierSummaryResponse::id, DossierSummaryResponse::numero));

        return new TiersHistoriqueResponse(
                dossiers,
                cotations,
                factures,
                paiements.stream().map(this::toPaiementResponse).toList(),
                marchandises.stream().map(m -> new TiersHistoriqueResponse.MarchandiseHistoriqueItem(
                        m.getId(), m.getDossierId(), numeroParDossier.getOrDefault(m.getDossierId(), "\u2014"),
                        m.getDesignation(), m.getStatut(), m.getCreatedAt())).toList(),
                documents.stream().map(d -> new TiersHistoriqueResponse.DocumentHistoriqueItem(
                        d.getId(), d.getDossierId(), numeroParDossier.getOrDefault(d.getDossierId(), "\u2014"),
                        d.getTitre(), d.getTypeDocument(), d.getDateAjout())).toList()
        );
    }

    private PaiementResponse toPaiementResponse(Paiement p) {
        return new PaiementResponse(p.getId(), p.getNumero(), p.getFactureId(), p.getCotationId(), p.getDossierId(), null,
                p.getClientId(), p.getMontant(), p.getDevise(), p.getModePaiement(), p.getDatePaiement(),
                p.getReference(), p.getStatut(), p.getObservations());
    }
}