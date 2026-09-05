package com.transit.platform.facture;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.facture.dto.*;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Statuts EN_ATTENTE / PARTIELLEMENT_PAYEE / PAYEE sont des états CALCULÉS, propriété de
 * PaiementService (Prompt 03 §26) — jamais assignables manuellement via changeStatut ici.
 * Montants toujours recalculés à partir des lignes réelles en base (§17, §34).
 */
@Service
public class FactureService {

    /** Statuts qu'un utilisateur peut positionner manuellement ; les autres sont dérivés des paiements. */
    private static final Set<String> STATUTS_MANUELS = Set.of("BROUILLON", "EMISE", "ANNULEE");

    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneRepository;
    private final TiersRepository tiersRepository;
    private final DossierService dossierService;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public FactureService(FactureRepository factureRepository, LigneFactureRepository ligneRepository,
                           TiersRepository tiersRepository, DossierService dossierService,
                           ReferenceGeneratorService referenceGeneratorService, TenantContext tenantContext,
                           AuditService auditService) {
        this.factureRepository = factureRepository;
        this.ligneRepository = ligneRepository;
        this.tiersRepository = tiersRepository;
        this.dossierService = dossierService;
        this.referenceGeneratorService = referenceGeneratorService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<FactureSummaryResponse> search(String statut, UUID clientId, UUID dossierId, String search, Pageable pageable) {
        String normalized = search == null ? null : "%" + search.toLowerCase() + "%";
        // Correctif audit (Prompt 07 §37) : "EN_RETARD" est un état dérivé, jamais stocké
        // tel quel — voir FactureRepository.searchEnRetard pour la justification complète.
        Page<Facture> page = "EN_RETARD".equals(statut)
                ? factureRepository.searchEnRetard(tenantContext.currentEntrepriseId(), clientId, dossierId, normalized, pageable)
                : factureRepository.search(tenantContext.currentEntrepriseId(), statut, clientId, dossierId, normalized, pageable);

        Set<UUID> dossierIds = page.getContent().stream().map(Facture::getDossierId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> numerosParDossier = dossierService.findNumerosByIds(dossierIds);

        return page.map(f -> toSummary(f, numerosParDossier.get(f.getDossierId())));
    }

    @Transactional(readOnly = true)
    public Page<FactureSummaryResponse> listByDossier(UUID dossierId, Pageable pageable) {
        dossierService.findWithinTenant(dossierId);
        return factureRepository.findByEntrepriseIdAndDossierIdAndDeletedAtIsNull(
                tenantContext.currentEntrepriseId(), dossierId, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public FactureResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional
    public FactureResponse create(CreateFactureRequest request) {
        return createInternal(null, request);
    }

    /**
     * Endpoint contextualisé POST /dossiers/{dossierId}/factures (Prompt 03 §21) :
     * 1) vérifie le dossier + son appartenance au tenant (via DossierService)
     * 2) déduit automatiquement le client du dossier
     * 3) déduit la devise par défaut de l'entreprise si non fournie
     * 4) génère le numéro
     * 5) crée la facture et ses lignes
     * 6) retourne la facture créée
     */
    @Transactional
    public FactureResponse createForDossier(UUID dossierId, CreateFactureRequest request) {
        return createInternal(dossierId, request);
    }

    private FactureResponse createInternal(UUID dossierIdFromPath, CreateFactureRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Dossier dossier = null;
        UUID resolvedClientId = request.clientId();

        if (dossierIdFromPath != null) {
            dossier = dossierService.findWithinTenant(dossierIdFromPath);
            resolvedClientId = resolvedClientId != null ? resolvedClientId : dossier.getClientId(); // client déduit automatiquement
        }
        if (resolvedClientId == null) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR,
                    "Le client est obligatoire lorsque la facture n'est pas créée depuis un dossier");
        }

        Tiers client = tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(resolvedClientId, entrepriseId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.CLIENT_NOT_FOUND, "Client introuvable pour cette entreprise"));

        Facture facture = new Facture();
        facture.setEntrepriseId(entrepriseId);
        facture.setNumero(referenceGeneratorService.generate(entrepriseId, ReferenceType.FACTURE)); // jamais saisi par l'utilisateur
        facture.setTypeDocument(request.typeDocument() != null ? request.typeDocument() : "FACTURE");
        facture.setClientId(client.getId());
        facture.setDossierId(dossier != null ? dossier.getId() : null);
        facture.setTitre(request.titre());
        facture.setDateEcheance(request.dateEcheance());
        facture.setDevise(request.devise() != null ? request.devise() : "XOF"); // devise par défaut si non fournie
        facture.setNotes(request.notes());
        facture.setConditions(request.conditions());
        facture.setStatut("BROUILLON");
        facture.setCreatedBy(tenantContext.currentUtilisateurId());
        facture = factureRepository.save(facture);

        int ordre = 0;
        for (LigneFactureRequest ligneReq : request.lignes()) {
            ligneRepository.save(buildLigne(facture.getId(), ligneReq, ordre++));
        }
        recalculateTotals(facture);
        facture = factureRepository.save(facture);

        if (dossier != null) {
            dossierService.demarrerSiOuvert(dossier);
            dossierService.recordHistorique(dossier, EvenementHistorique.FACTURE_CREATED, "Facture " + facture.getNumero() + " créée");
        }
        auditService.log("CREATE", "FACTURE", facture.getId(), null, Map.of("numero", facture.getNumero()));

        return toResponse(facture);
    }

    /**
     * Transforme une cotation acceptée en facture (demande utilisateur) : copie client,
     * dossier, devise, notes, conditions et toutes les lignes — l'utilisateur n'a rien à
     * ressaisir. La facture reste en BROUILLON (elle doit être émise manuellement avant
     * paiement, comme toute facture — cf. garde-fou déjà en place dans PaiementService).
     */
    @Transactional
    public FactureResponse createFromCotation(com.transit.platform.cotation.Cotation cotation,
                                              List<com.transit.platform.cotation.LigneCotation> lignesCotation) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Tiers client = tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(cotation.getClientId(), entrepriseId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TIERS_NOT_FOUND, "Client introuvable pour cette entreprise"));

        Facture facture = new Facture();
        facture.setEntrepriseId(entrepriseId);
        facture.setNumero(referenceGeneratorService.generate(entrepriseId, ReferenceType.FACTURE));
        facture.setTypeDocument("FACTURE");
        facture.setClientId(client.getId());
        facture.setDossierId(cotation.getDossierId());
        facture.setCotationId(cotation.getId());
        facture.setTitre(cotation.getTitre());
        facture.setDevise(cotation.getDevise() != null ? cotation.getDevise() : "XOF");
        facture.setNotes(cotation.getNotes());
        facture.setConditions(cotation.getConditions());
        facture.setStatut("BROUILLON");
        facture.setDateDocument(java.time.LocalDate.now());
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setCreatedBy(tenantContext.currentUtilisateurId());
        facture = factureRepository.save(facture);

        int ordre = 0;
        for (com.transit.platform.cotation.LigneCotation l : lignesCotation) {
            LigneFacture lf = new LigneFacture();
            lf.setFactureId(facture.getId());
            lf.setCategorieFrais(l.getCategorieFrais());
            lf.setDescription(l.getDescription());
            lf.setQuantite(l.getQuantite());
            lf.setPrixUnitaire(l.getPrixUnitaire());
            lf.setTauxTaxe(l.getTauxTaxe());
            lf.setMontant(l.getMontant());
            lf.setMontantTaxe(l.getMontantTaxe());
            lf.setOrdre(ordre++);
            ligneRepository.save(lf);
        }
        recalculateTotals(facture);
        facture = factureRepository.save(facture);

        if (facture.getDossierId() != null) {
            Dossier dossier = dossierService.findWithinTenant(facture.getDossierId());
            dossierService.demarrerSiOuvert(dossier);
            dossierService.recordHistorique(dossier, EvenementHistorique.FACTURE_CREATED,
                    "Facture " + facture.getNumero() + " créée depuis la cotation " + cotation.getNumero());
        }
        auditService.log("CREATE", "FACTURE", facture.getId(), null,
                Map.of("numero", facture.getNumero(), "cotationId", cotation.getId().toString()));

        return toResponse(facture);
    }

    @Transactional
    public FactureResponse update(UUID id, UpdateFactureRequest request) {
        Facture facture = findWithinTenant(id);
        ensureModifiable(facture);

        if (request.clientId() != null) {
            tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(request.clientId(), tenantContext.currentEntrepriseId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.CLIENT_NOT_FOUND, "Client introuvable pour cette entreprise"));
            facture.setClientId(request.clientId());
        }
        facture.setTitre(request.titre());
        facture.setDateEcheance(request.dateEcheance());
        if (request.devise() != null) facture.setDevise(request.devise());
        facture.setNotes(request.notes());
        facture.setConditions(request.conditions());
        facture.setUpdatedAt(java.time.Instant.now());
        return toResponse(factureRepository.save(facture));
    }

    @Transactional
    public FactureResponse changeStatut(UUID id, ChangeStatutFactureRequest request) {
        Facture facture = findWithinTenant(id);
        if (!STATUTS_MANUELS.contains(request.statut())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Statut '" + request.statut() + "' calculé automatiquement selon les paiements — non assignable manuellement");
        }
        String ancien = facture.getStatut();
        facture.setStatut(request.statut());
        facture.setUpdatedAt(java.time.Instant.now());
        facture = factureRepository.save(facture);
        auditService.log("UPDATE_STATUS", "FACTURE", facture.getId(), Map.of("statut", ancien), Map.of("statut", request.statut()));
        return toResponse(facture);
    }

    /** Cas critique n°7 du Prompt 03 §47 : une facture déjà payée (même partiellement) ne peut jamais être supprimée. */
    @Transactional
    public void delete(UUID id) {
        Facture facture = findWithinTenant(id);
        ensureModifiable(facture);
        if (facture.getMontantPaye().compareTo(BigDecimal.ZERO) > 0) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Impossible de supprimer une facture ayant déjà reçu un paiement");
        }
        facture.setDeletedAt(java.time.Instant.now());
        factureRepository.save(facture);
        auditService.log("DELETE", "FACTURE", facture.getId(), Map.of("numero", facture.getNumero()), null);
    }

    @Transactional
    public FactureResponse addLigne(UUID factureId, LigneFactureRequest request) {
        Facture facture = findWithinTenant(factureId);
        ensureModifiable(facture);
        int ordre = ligneRepository.findByFactureIdOrderByOrdreAsc(factureId).size();
        ligneRepository.save(buildLigne(factureId, request, ordre));
        recalculateTotals(facture);
        return toResponse(factureRepository.save(facture));
    }

    @Transactional
    public FactureResponse updateLigne(UUID factureId, UUID ligneId, LigneFactureRequest request) {
        Facture facture = findWithinTenant(factureId);
        ensureModifiable(facture);
        LigneFacture ligne = ligneRepository.findByIdAndFactureId(ligneId, factureId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.FACTURE_NOT_FOUND, "Ligne de facture introuvable"));
        applyLigneFields(ligne, request);
        ligneRepository.save(ligne);
        recalculateTotals(facture);
        return toResponse(factureRepository.save(facture));
    }

    @Transactional
    public FactureResponse deleteLigne(UUID factureId, UUID ligneId) {
        Facture facture = findWithinTenant(factureId);
        ensureModifiable(facture);
        LigneFacture ligne = ligneRepository.findByIdAndFactureId(ligneId, factureId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.FACTURE_NOT_FOUND, "Ligne de facture introuvable"));
        ligneRepository.delete(ligne);
        recalculateTotals(facture);
        return toResponse(factureRepository.save(facture));
    }

    /**
     * Utilisé par PaiementService après chaque paiement/annulation pour resynchroniser
     * montantPaye/resteAPayer/statut à partir de la réalité des paiements en base — jamais
     * l'inverse (aucun service ne doit écrire directement ces trois champs ailleurs).
     */
    @Transactional
    public void recalculerApresPaiement(UUID factureId, BigDecimal nouveauMontantPaye) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.FACTURE_NOT_FOUND, "Facture introuvable"));

        facture.setMontantPaye(nouveauMontantPaye);
        facture.setResteAPayer(facture.getMontantTotal().subtract(nouveauMontantPaye));

        if (nouveauMontantPaye.compareTo(BigDecimal.ZERO) <= 0) {
            facture.setStatut("EMISE");
        } else if (nouveauMontantPaye.compareTo(facture.getMontantTotal()) >= 0) {
            facture.setStatut("PAYEE");
        } else {
            facture.setStatut("PARTIELLEMENT_PAYEE");
        }
        facture.setUpdatedAt(java.time.Instant.now());
        factureRepository.save(facture);
    }

    /** Utilisé par PaiementService pour valider facture + tenant avant d'accepter un paiement. */
    @Transactional(readOnly = true)
    public Facture findWithinTenant(UUID id) {
        return factureRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.FACTURE_NOT_FOUND, "Facture introuvable"));
    }

    private void recalculateTotals(Facture facture) {
        List<LigneFacture> lignes = ligneRepository.findByFactureIdOrderByOrdreAsc(facture.getId());
        BigDecimal ht = lignes.stream().map(LigneFacture::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxe = lignes.stream().map(LigneFacture::getMontantTaxe).reduce(BigDecimal.ZERO, BigDecimal::add);
        facture.setMontantHT(ht);
        facture.setMontantTaxe(taxe);
        facture.setMontantTotal(ht.add(taxe));
        facture.setResteAPayer(ht.add(taxe).subtract(facture.getMontantPaye()));
        facture.setUpdatedAt(java.time.Instant.now());
    }

    private LigneFacture buildLigne(UUID factureId, LigneFactureRequest request, int ordre) {
        LigneFacture ligne = new LigneFacture();
        ligne.setFactureId(factureId);
        ligne.setOrdre(ordre);
        applyLigneFields(ligne, request);
        return ligne;
    }

    private void applyLigneFields(LigneFacture ligne, LigneFactureRequest request) {
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
        ligne.setMontant(montant);
        ligne.setMontantTaxe(montantTaxe);
    }

    private void ensureModifiable(Facture facture) {
        if (!"BROUILLON".equals(facture.getStatut())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Seule une facture au statut BROUILLON peut être modifiée ou supprimée");
        }
    }

    private FactureResponse toResponse(Facture f) {
        List<LigneFactureResponse> lignes = ligneRepository.findByFactureIdOrderByOrdreAsc(f.getId()).stream()
                .map(l -> new LigneFactureResponse(l.getId(), l.getCategorieFrais(), l.getDescription(), l.getQuantite(),
                        l.getPrixUnitaire(), l.getMontant(), l.getTauxTaxe(), l.getMontantTaxe()))
                .toList();
        return new FactureResponse(f.getId(), f.getNumero(), f.getTypeDocument(), f.getClientId(), f.getDossierId(),
                f.getCotationId(), f.getTitre(), f.getDateDocument(), f.getDateEcheance(), f.getDevise(), statutAffiche(f),
                f.getMontantHT(), f.getMontantTaxe(), f.getMontantTotal(), f.getMontantPaye(), f.getResteAPayer(),
                f.getNotes(), f.getConditions(), lignes);
    }

    private FactureSummaryResponse toSummary(Facture f) {
        return toSummary(f, null);
    }

    private FactureSummaryResponse toSummary(Facture f, String dossierNumero) {
        return new FactureSummaryResponse(f.getId(), f.getNumero(), f.getTypeDocument(), f.getClientId(), f.getDossierId(),
                dossierNumero, statutAffiche(f), f.getMontantTotal(), f.getResteAPayer(), f.getDevise(), f.getDateDocument(), f.getDateEcheance());
    }

    /**
     * Correctif audit (Prompt 07 §37/§65) : calcule le statut EFFECTIVEMENT affiché aux
     * clients (Angular/Flutter), en superposant "EN_RETARD" par-dessus EMISE/
     * PARTIELLEMENT_PAYEE quand l'échéance est dépassée et qu'il reste un solde dû — sans
     * jamais modifier la colonne persistée (qui reste la référence pour les transitions
     * manuelles autorisées, cf. STATUTS_MANUELS). Les deux frontends attendaient déjà cette
     * valeur (types TypeScript/constantes Dart), ce correctif les rend enfin atteignables
     * sans aucune modification côté client.
     */
    private String statutAffiche(Facture f) {
        boolean enRetard = f.getDateEcheance() != null
                && f.getDateEcheance().isBefore(java.time.LocalDate.now())
                && f.getResteAPayer().compareTo(BigDecimal.ZERO) > 0
                && ("EMISE".equals(f.getStatut()) || "PARTIELLEMENT_PAYEE".equals(f.getStatut()));
        return enRetard ? "EN_RETARD" : f.getStatut();
    }
}
