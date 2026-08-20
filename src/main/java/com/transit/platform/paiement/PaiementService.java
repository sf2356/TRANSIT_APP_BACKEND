package com.transit.platform.paiement;

import com.transit.platform.audit.AuditService;
import com.transit.platform.caisse.CaisseService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.common.idempotency.IdempotencyService;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierRepository;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.facture.Facture;
import com.transit.platform.facture.FactureService;
import com.transit.platform.paiement.dto.*;
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
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Opération financière la plus sensible de la plateforme (Prompt 03 §52). Chaque création
 * de paiement suit STRICTEMENT la séquence : vérifier facture → vérifier client → vérifier
 * montant → enregistrer paiement → recalculer facture → mettre à jour statut → mouvement de
 * caisse éventuel → audit — le tout dans une seule transaction (@Transactional par défaut,
 * REQUIRES_NEW seulement pour la génération de numéro et l'audit, cf. ReferenceGeneratorService
 * et AuditService).
 */
@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final FactureService factureService;
    private final TiersRepository tiersRepository;
    private final DossierService dossierService;
    private final DossierRepository dossierRepository;
    private final CaisseService caisseService;
    private final IdempotencyService idempotencyService;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public PaiementService(PaiementRepository paiementRepository, FactureService factureService,
                            TiersRepository tiersRepository, DossierService dossierService, CaisseService caisseService,
                            IdempotencyService idempotencyService, ReferenceGeneratorService referenceGeneratorService,
                            TenantContext tenantContext, AuditService auditService,DossierRepository dossierRepository) {
        this.paiementRepository = paiementRepository;
        this.factureService = factureService;
        this.tiersRepository = tiersRepository;
        this.dossierService = dossierService;
        this.caisseService = caisseService;
        this.idempotencyService = idempotencyService;
        this.referenceGeneratorService = referenceGeneratorService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.dossierRepository = dossierRepository;
    }

    @Transactional(readOnly = true)
    public Page<PaiementResponse> search(UUID factureId, UUID dossierId, String statut, Pageable pageable) {
        Page<Paiement> page = paiementRepository.search(tenantContext.currentEntrepriseId(), factureId, dossierId, statut, pageable);
        java.util.Set<UUID> dossierIds = page.getContent().stream()
                .map(Paiement::getDossierId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Map<UUID, String> numeros = dossierRepository.findAllById(dossierIds).stream()
                .collect(java.util.stream.Collectors.toMap(com.transit.platform.dossier.Dossier::getId, com.transit.platform.dossier.Dossier::getNumero));
        return page.map(p -> toResponse(p, numeros.get(p.getDossierId())));
    }

    @Transactional(readOnly = true)
    public PaiementResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional(readOnly = true)
    public Page<PaiementResponse> listByFacture(UUID factureId, Pageable pageable) {
        factureService.findWithinTenant(factureId);
        return paiementRepository.search(tenantContext.currentEntrepriseId(), factureId, null, null, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PaiementResponse create(CreatePaiementRequest request, String idempotencyKey) {
        if (request.factureId() == null && request.cotationId() == null && request.dossierId() == null) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR,
                    "Un paiement doit être rattaché à une facture, une cotation ou un dossier");
        }

        UUID entrepriseId = tenantContext.currentEntrepriseId();
        var existing = idempotencyService.findExistingResourceId(entrepriseId, idempotencyKey, "POST /paiements");
        if (existing.isPresent()) {
            return toResponse(paiementRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(existing.get(), entrepriseId)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.PAIEMENT_NOT_FOUND, "Paiement introuvable")));
        }

        PaiementResponse response = createInternal(request.factureId(), request.cotationId(), request.dossierId(), request.clientId(),
                request.montant(), request.devise(), request.modePaiement(), request.datePaiement(),
                request.reference(), request.observations());
        idempotencyService.record(entrepriseId, idempotencyKey, "POST /paiements", "PAIEMENT", response.id());
        return response;
    }

    /** Endpoint contextualisé POST /factures/{id}/paiements — client et dossier déduits de la facture (Prompt 03 §19, §21). */
    @Transactional
    public PaiementResponse createForFacture(UUID factureId, PaiementFactureRequest request, String idempotencyKey) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        String endpoint = "POST /factures/" + factureId + "/paiements";
        var existing = idempotencyService.findExistingResourceId(entrepriseId, idempotencyKey, endpoint);
        if (existing.isPresent()) {
            return toResponse(paiementRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(existing.get(), entrepriseId)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.PAIEMENT_NOT_FOUND, "Paiement introuvable")));
        }

        PaiementResponse response = createInternal(factureId, null, null, null, request.montant(), null, request.modePaiement(),
                request.datePaiement(), request.reference(), request.observations());
        idempotencyService.record(entrepriseId, idempotencyKey, endpoint, "PAIEMENT", response.id());
        return response;
    }

    private PaiementResponse createInternal(UUID factureId, UUID cotationId, UUID dossierId, UUID clientIdRequested,
                                              BigDecimal montant, String devise, String modePaiement,
                                              LocalDate datePaiement, String reference, String observations) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();

        // 1) vérifier la facture (si fournie) — appartenance au tenant garantie par FactureService
        Facture facture = null;
        if (factureId != null) {
            facture = factureService.findWithinTenant(factureId);
            // Correctif audit (Prompt 07 §17/§37/§65) : une facture BROUILLON n'a jamais été
            // "émise" auprès du client — rien ne devrait pouvoir être payé avant cette étape.
            // Ce garde-fou était absent : un paiement pouvait jusqu'ici être enregistré sur
            // une facture encore en brouillon, contournant silencieusement le statut.
            if ("BROUILLON".equals(facture.getStatut())) {
                throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                        "Impossible d'enregistrer un paiement sur une facture encore au statut BROUILLON. "
                                + "Émettez d'abord la facture (statut EMISE).");
            }
        }

        // dossier/client déduits de la facture si elle est fournie, sinon résolus explicitement
        UUID resolvedDossierId = facture != null ? facture.getDossierId() : dossierId;
        UUID resolvedClientId = facture != null ? facture.getClientId() : clientIdRequested;
        String resolvedDevise = devise != null ? devise : (facture != null ? facture.getDevise() : "XOF");

        if (resolvedDossierId != null) {
            dossierService.findWithinTenant(resolvedDossierId); // valide aussi le tenant si dossier fourni sans facture
        }
        if (resolvedClientId == null) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR,
                    "Le client est obligatoire lorsque le paiement n'est pas rattaché à une facture");
        }

        // 2) vérifier le client (appartenance au tenant)
        Tiers client = tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(resolvedClientId, entrepriseId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.CLIENT_NOT_FOUND, "Client introuvable pour cette entreprise"));

        // 3) entreprise déjà garantie par TenantContext (aucune valeur venant du client HTTP)

        // 4) vérifier le montant — règle retenue : un paiement ne peut jamais dépasser le reste à payer
        // d'une facture (Prompt 03 §26/§32 : "ne pas accepter une marge incohérente"). Documentée au
        // README comme décision à valider avec le métier — l'alternative (avoir client/note de crédit)
        // pourra être ajoutée en V2 sans changer cette structure.
        if (facture != null && montant.compareTo(facture.getResteAPayer()) > 0) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_AMOUNT,
                    "Le montant du paiement (" + montant + ") dépasse le reste à payer de la facture (" + facture.getResteAPayer() + ")");
        }

        // 5) enregistrer le paiement
        Paiement paiement = new Paiement();
        paiement.setEntrepriseId(entrepriseId);
        paiement.setNumero(referenceGeneratorService.generate(entrepriseId, ReferenceType.PAIEMENT)); // jamais saisi par l'utilisateur
        paiement.setFactureId(facture != null ? facture.getId() : null);
        paiement.setCotationId(cotationId);
        paiement.setDossierId(resolvedDossierId);
        paiement.setClientId(client.getId());
        paiement.setMontant(montant);
        paiement.setDevise(resolvedDevise);
        paiement.setModePaiement(modePaiement);
        paiement.setDatePaiement(datePaiement != null ? datePaiement : LocalDate.now());
        paiement.setReference(reference);
        paiement.setObservations(observations);
        paiement.setStatut("VALIDE");
        paiement.setCreatedBy(tenantContext.currentUtilisateurId());
        paiement = paiementRepository.save(paiement);

        // 6) + 7) recalculer la facture et son statut à partir de la réalité des paiements en base
        if (facture != null) {
            BigDecimal nouveauMontantPaye = paiementRepository.sumValidePourFacture(facture.getId());
            factureService.recalculerApresPaiement(facture.getId(), nouveauMontantPaye);
        }

        // 8) mouvement de caisse éventuel (espèces uniquement — cf. CaisseService.creerDepuisPaiement)
        if ("ESPECES".equals(modePaiement)) {
            caisseService.creerDepuisPaiement(entrepriseId, resolvedDossierId, paiement.getId(), montant, resolvedDevise,
                    modePaiement, "Encaissement " + paiement.getNumero(), tenantContext.currentUtilisateurId());
        }

        // 9) historique dossier + audit
        if (resolvedDossierId != null) {
            Dossier dossier = dossierService.findWithinTenant(resolvedDossierId);
            dossierService.recordHistorique(dossier, EvenementHistorique.PAIEMENT_RECEIVED,
                    "Paiement " + paiement.getNumero() + " enregistré (" + montant + " " + resolvedDevise + ")");
        }
        auditService.log("CREATE", "PAIEMENT", paiement.getId(), null,
                Map.of("numero", paiement.getNumero(), "montant", paiement.getMontant()));

        return toResponse(paiement);
    }

    /** Annulation : retire le paiement du calcul de la facture et neutralise le mouvement de caisse associé. */
    @Transactional
    public void annuler(UUID id) {
        Paiement paiement = findWithinTenant(id);
        if ("ANNULE".equals(paiement.getStatut())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS, "Ce paiement est déjà annulé");
        }

        paiement.setStatut("ANNULE");
        paiement.setUpdatedAt(java.time.Instant.now());
        paiementRepository.save(paiement);

        if (paiement.getFactureId() != null) {
            BigDecimal nouveauMontantPaye = paiementRepository.sumValidePourFacture(paiement.getFactureId());
            factureService.recalculerApresPaiement(paiement.getFactureId(), nouveauMontantPaye);
        }

        auditService.log("CANCEL", "PAIEMENT", paiement.getId(), Map.of("statut", "VALIDE"), Map.of("statut", "ANNULE"));
    }

    private Paiement findWithinTenant(UUID id) {
        return paiementRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.PAIEMENT_NOT_FOUND, "Paiement introuvable"));
    }

    private PaiementResponse toResponse(Paiement p) {
        String dossierNumero = p.getDossierId() != null
                ? dossierRepository.findById(p.getDossierId()).map(com.transit.platform.dossier.Dossier::getNumero).orElse(null)
                : null;
        return toResponse(p, dossierNumero);
    }

    private PaiementResponse toResponse(Paiement p, String dossierNumero) {
        return new PaiementResponse(p.getId(), p.getNumero(), p.getFactureId(), p.getCotationId(), p.getDossierId(), dossierNumero,
                p.getClientId(), p.getMontant(), p.getDevise(), p.getModePaiement(), p.getDatePaiement(),
                p.getReference(), p.getStatut(), p.getObservations());
    }
}
