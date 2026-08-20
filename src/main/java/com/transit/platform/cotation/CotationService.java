package com.transit.platform.cotation;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.cotation.dto.*;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.reference.ReferenceGeneratorService;
import com.transit.platform.reference.ReferenceType;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les montants (ligne, HT, taxe, total) sont TOUJOURS calculés ici, jamais acceptés tels
 * quels depuis le client (Prompt 03 §17, §24, §34). Toute mutation de lignes déclenche un
 * recalcul complet de l'en-tête pour garantir la cohérence — pas de mise à jour partielle
 * qui pourrait désynchroniser montantTotal des lignes réelles.
 */
@Service
public class CotationService {

    private final CotationRepository cotationRepository;
    private final LigneCotationRepository ligneRepository;
    private final TiersRepository tiersRepository;
    private final DossierService dossierService;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final com.transit.platform.facture.FactureService factureService;

    public CotationService(CotationRepository cotationRepository, LigneCotationRepository ligneRepository,
                           TiersRepository tiersRepository, DossierService dossierService,
                           ReferenceGeneratorService referenceGeneratorService, TenantContext tenantContext,
                           AuditService auditService, com.transit.platform.facture.FactureService factureService) {
        this.cotationRepository = cotationRepository;
        this.ligneRepository = ligneRepository;
        this.tiersRepository = tiersRepository;
        this.dossierService = dossierService;
        this.referenceGeneratorService = referenceGeneratorService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.factureService = factureService;
    }

    @Transactional
    public com.transit.platform.facture.dto.FactureResponse facturer(UUID cotationId) {
        Cotation cotation = findWithinTenant(cotationId);
        if (!"ACCEPTEE".equals(cotation.getStatut())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Seule une cotation acceptée peut être transformée en facture (statut actuel : " + cotation.getStatut() + ")");
        }
        List<LigneCotation> lignes = ligneRepository.findByCotationIdOrderByOrdreAsc(cotation.getId());
        if (lignes.isEmpty()) {
            throw BusinessException.unprocessable(ErrorCode.VALIDATION_ERROR, "La cotation ne contient aucune ligne à facturer");
        }
        return factureService.createFromCotation(cotation, lignes);
    }

    @Transactional(readOnly = true)
    public Page<CotationSummaryResponse> search(String statut, UUID clientId, UUID dossierId, String search, Pageable pageable) {
        String normalized = search == null ? null : "%" + search.toLowerCase() + "%";
        return cotationRepository.search(tenantContext.currentEntrepriseId(), statut, clientId, dossierId, normalized, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<CotationSummaryResponse> listByDossier(UUID dossierId, Pageable pageable) {
        dossierService.findWithinTenant(dossierId);
        return cotationRepository.findByEntrepriseIdAndDossierId(tenantContext.currentEntrepriseId(), dossierId, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public CotationResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional
    public CotationResponse create(CreateCotationRequest request) {
        return createInternal(null, request);
    }

    /** Endpoint contextualisé POST /dossiers/{dossierId}/cotations — client déduit automatiquement (Prompt 01 §9). */
    @Transactional
    public CotationResponse createForDossier(UUID dossierId, CreateCotationRequest request) {
        return createInternal(dossierId, request);
    }

    private CotationResponse createInternal(UUID dossierIdFromPath, CreateCotationRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Dossier dossier = null;
        UUID resolvedClientId = request.clientId();

        if (dossierIdFromPath != null) {
            dossier = dossierService.findWithinTenant(dossierIdFromPath);
            resolvedClientId = resolvedClientId != null ? resolvedClientId : dossier.getClientId();
        }
        if (resolvedClientId == null) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR, "Le client est obligatoire lorsque la cotation n'est pas créée depuis un dossier");
        }

        Tiers client = tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(resolvedClientId, entrepriseId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.CLIENT_NOT_FOUND, "Client introuvable pour cette entreprise"));

        Cotation cotation = new Cotation();
        cotation.setEntrepriseId(entrepriseId);
        cotation.setNumero(referenceGeneratorService.generate(entrepriseId, ReferenceType.COTATION)); // jamais saisi par l'utilisateur
        cotation.setClientId(client.getId());
        cotation.setDossierId(dossier != null ? dossier.getId() : null);
        cotation.setTitre(request.titre());
        cotation.setDateValidite(request.dateValidite());
        cotation.setDevise(request.devise() != null ? request.devise() : "XOF");
        cotation.setNotes(request.notes());
        cotation.setConditions(request.conditions());
        cotation.setStatut("BROUILLON");
        cotation.setCreatedBy(tenantContext.currentUtilisateurId());
        cotation = cotationRepository.save(cotation);

        int ordre = 0;
        for (LigneCotationRequest ligneReq : request.lignes()) {
            ligneRepository.save(buildLigne(cotation.getId(), ligneReq, ordre++));
        }
        recalculateTotals(cotation);
        cotation = cotationRepository.save(cotation);

        if (dossier != null) {
            dossierService.demarrerSiOuvert(dossier);
            dossierService.recordHistorique(dossier, EvenementHistorique.COTATION_CREATED, "Cotation " + cotation.getNumero() + " créée");
        }
        auditService.log("CREATE", "COTATION", cotation.getId(), null, Map.of("numero", cotation.getNumero()));

        return toResponse(cotation);
    }

    @Transactional
    public CotationResponse update(UUID id, UpdateCotationRequest request) {
        Cotation cotation = findWithinTenant(id);
        ensureModifiable(cotation);

        if (request.clientId() != null) {
            tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(request.clientId(), tenantContext.currentEntrepriseId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.CLIENT_NOT_FOUND, "Client introuvable pour cette entreprise"));
            cotation.setClientId(request.clientId());
        }
        cotation.setTitre(request.titre());
        cotation.setDateValidite(request.dateValidite());
        if (request.devise() != null) cotation.setDevise(request.devise());
        cotation.setNotes(request.notes());
        cotation.setConditions(request.conditions());
        cotation.setUpdatedAt(java.time.Instant.now());
        return toResponse(cotationRepository.save(cotation));
    }

    @Transactional
    public CotationResponse changeStatut(UUID id, ChangeStatutCotationRequest request) {
        Cotation cotation = findWithinTenant(id);
        cotation.setStatut(request.statut());
        cotation.setUpdatedAt(java.time.Instant.now());
        cotation = cotationRepository.save(cotation);
        auditService.log("UPDATE_STATUS", "COTATION", cotation.getId(), null, Map.of("statut", request.statut()));
        return toResponse(cotation);
    }

    @Transactional
    public void delete(UUID id) {
        Cotation cotation = findWithinTenant(id);
        ensureModifiable(cotation);
        ligneRepository.deleteByCotationId(cotation.getId());
        cotationRepository.delete(cotation);
        auditService.log("DELETE", "COTATION", cotation.getId(), Map.of("numero", cotation.getNumero()), null);
    }

    @Transactional
    public CotationResponse addLigne(UUID cotationId, LigneCotationRequest request) {
        Cotation cotation = findWithinTenant(cotationId);
        ensureModifiable(cotation);
        int ordre = ligneRepository.findByCotationIdOrderByOrdreAsc(cotationId).size();
        ligneRepository.save(buildLigne(cotationId, request, ordre));
        recalculateTotals(cotation);
        return toResponse(cotationRepository.save(cotation));
    }

    @Transactional
    public CotationResponse updateLigne(UUID cotationId, UUID ligneId, LigneCotationRequest request) {
        Cotation cotation = findWithinTenant(cotationId);
        ensureModifiable(cotation);
        LigneCotation ligne = ligneRepository.findByIdAndCotationId(ligneId, cotationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.COTATION_NOT_FOUND, "Ligne de cotation introuvable"));
        applyLigneFields(ligne, request);
        ligneRepository.save(ligne);
        recalculateTotals(cotation);
        return toResponse(cotationRepository.save(cotation));
    }

    @Transactional
    public CotationResponse deleteLigne(UUID cotationId, UUID ligneId) {
        Cotation cotation = findWithinTenant(cotationId);
        ensureModifiable(cotation);
        LigneCotation ligne = ligneRepository.findByIdAndCotationId(ligneId, cotationId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.COTATION_NOT_FOUND, "Ligne de cotation introuvable"));
        ligneRepository.delete(ligne);
        recalculateTotals(cotation);
        return toResponse(cotationRepository.save(cotation));
    }

    /** Recalcule montantHT/montantTaxe/montantTotal à partir des lignes actuelles en base — jamais de confiance dans une valeur transmise. */
    private void recalculateTotals(Cotation cotation) {
        List<LigneCotation> lignes = ligneRepository.findByCotationIdOrderByOrdreAsc(cotation.getId());
        BigDecimal ht = lignes.stream().map(LigneCotation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxe = lignes.stream().map(LigneCotation::getMontantTaxe).reduce(BigDecimal.ZERO, BigDecimal::add);
        cotation.setMontantHT(ht);
        cotation.setMontantTaxe(taxe);
        cotation.setMontantTotal(ht.add(taxe));
        cotation.setUpdatedAt(java.time.Instant.now());
    }

    private LigneCotation buildLigne(UUID cotationId, LigneCotationRequest request, int ordre) {
        LigneCotation ligne = new LigneCotation();
        ligne.setCotationId(cotationId);
        ligne.setOrdre(ordre);
        applyLigneFields(ligne, request);
        return ligne;
    }

    private void applyLigneFields(LigneCotation ligne, LigneCotationRequest request) {
        ligne.setCategorieFrais(request.categorieFrais());
        ligne.setDescription(request.description());
        BigDecimal quantite = request.quantite() != null ? request.quantite() : BigDecimal.ONE;
        BigDecimal prixUnitaire = request.prixUnitaire();
        BigDecimal tauxTaxe = request.tauxTaxe() != null ? request.tauxTaxe() : BigDecimal.ZERO;

        BigDecimal montant = quantite.multiply(prixUnitaire).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantTaxe = montant.multiply(tauxTaxe).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(prixUnitaire);
        ligne.setTauxTaxe(tauxTaxe);
        ligne.setMontant(montant);       // calculé, jamais accepté du client
        ligne.setMontantTaxe(montantTaxe); // calculé, jamais accepté du client
    }

    private void ensureModifiable(Cotation cotation) {
        if (!"BROUILLON".equals(cotation.getStatut())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Seule une cotation au statut BROUILLON peut être modifiée");
        }
    }

    private Cotation findWithinTenant(UUID id) {
        return cotationRepository.findByIdAndEntrepriseId(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.COTATION_NOT_FOUND, "Cotation introuvable"));
    }

    private CotationResponse toResponse(Cotation c) {
        List<LigneCotationResponse> lignes = ligneRepository.findByCotationIdOrderByOrdreAsc(c.getId()).stream()
                .map(l -> new LigneCotationResponse(l.getId(), l.getCategorieFrais(), l.getDescription(), l.getQuantite(),
                        l.getPrixUnitaire(), l.getMontant(), l.getTauxTaxe(), l.getMontantTaxe()))
                .toList();
        return new CotationResponse(c.getId(), c.getNumero(), c.getClientId(), c.getDossierId(), c.getTitre(),
                c.getDateCotation(), c.getDateValidite(), c.getDevise(), c.getStatut(), c.getMontantHT(),
                c.getMontantTaxe(), c.getMontantTotal(), c.getNotes(), c.getConditions(), lignes);
    }

    private CotationSummaryResponse toSummary(Cotation c) {
        return new CotationSummaryResponse(c.getId(), c.getNumero(), c.getClientId(), c.getDossierId(), c.getStatut(),
                c.getMontantTotal(), c.getDevise(), c.getDateCotation());
    }
}
