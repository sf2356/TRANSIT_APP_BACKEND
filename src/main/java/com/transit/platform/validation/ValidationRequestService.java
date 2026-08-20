package com.transit.platform.validation;

import com.transit.platform.audit.AuditService;
import com.transit.platform.charge.ChargeRepository;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.cotation.CotationRepository;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.facture.FactureService;
import com.transit.platform.paiement.PaiementRepository;
import com.transit.platform.security.TenantContext;
import com.transit.platform.validation.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "entite_id" est une référence polymorphe SANS contrainte FK physique (Prompt 02 §44,
 * assumé). L'intégrité est donc garantie EXCLUSIVEMENT ici : avant toute création, on
 * vérifie que l'entité désignée existe réellement ET appartient au tenant courant, en
 * délégant au service/repository du module concerné plutôt qu'en dupliquant le contrôle.
 */
@Service
public class ValidationRequestService {

    private static final Set<String> ENTITE_TYPES_SUPPORTEES = Set.of("DOSSIER", "FACTURE", "COTATION", "PAIEMENT", "CHARGE");

    private final ValidationRequestRepository validationRepository;
    private final DossierService dossierService;
    private final FactureService factureService;
    private final CotationRepository cotationRepository;
    private final PaiementRepository paiementRepository;
    private final ChargeRepository chargeRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public ValidationRequestService(ValidationRequestRepository validationRepository, DossierService dossierService,
                                     FactureService factureService, CotationRepository cotationRepository,
                                     PaiementRepository paiementRepository, ChargeRepository chargeRepository,
                                     TenantContext tenantContext, AuditService auditService) {
        this.validationRepository = validationRepository;
        this.dossierService = dossierService;
        this.factureService = factureService;
        this.cotationRepository = cotationRepository;
        this.paiementRepository = paiementRepository;
        this.chargeRepository = chargeRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ValidationResponse> search(String statut, String entiteType, UUID demandeurId, Pageable pageable) {
        return validationRepository.search(tenantContext.currentEntrepriseId(), statut, entiteType, demandeurId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ValidationResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional
    public ValidationResponse create(CreateValidationRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        String entiteType = request.entiteType().toUpperCase();

        if (!ENTITE_TYPES_SUPPORTEES.contains(entiteType)) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR,
                    "Type d'entité non supporté pour une validation : " + entiteType);
        }
        verifierExistenceEtTenant(entiteType, request.entiteId(), entrepriseId);

        ValidationRequest validation = new ValidationRequest();
        validation.setEntrepriseId(entrepriseId);
        validation.setType(request.type());
        validation.setEntiteType(entiteType);
        validation.setEntiteId(request.entiteId());
        validation.setDemandeurId(tenantContext.currentUtilisateurId());
        validation.setCommentaire(request.commentaire());
        validation.setStatut("EN_ATTENTE");
        validation = validationRepository.save(validation);

        if ("DOSSIER".equals(entiteType)) {
            var dossier = dossierService.findWithinTenant(request.entiteId());
            dossierService.recordHistorique(dossier, EvenementHistorique.VALIDATION_REQUESTED,
                    "Validation demandée : " + request.type());
        }
        auditService.log("CREATE", "VALIDATION", validation.getId(), null,
                Map.of("type", validation.getType(), "entiteType", entiteType));

        return toResponse(validation);
    }

    @Transactional
    public ValidationResponse approve(UUID id, DecisionValidationRequest request) {
        return decide(id, "APPROUVEE", request.commentaire());
    }

    @Transactional
    public ValidationResponse reject(UUID id, DecisionValidationRequest request) {
        return decide(id, "REJETEE", request.commentaire());
    }

    private ValidationResponse decide(UUID id, String statut, String commentaire) {
        ValidationRequest validation = findWithinTenant(id);
        if (!"EN_ATTENTE".equals(validation.getStatut())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Cette demande de validation a déjà été traitée (" + validation.getStatut() + ")");
        }
        validation.setStatut(statut);
        validation.setValidateurId(tenantContext.currentUtilisateurId());
        validation.setCommentaire(commentaire != null ? commentaire : validation.getCommentaire());
        validation.setDateDecision(Instant.now());
        validation = validationRepository.save(validation);

        auditService.log("DECIDE", "VALIDATION", validation.getId(),
                Map.of("statut", "EN_ATTENTE"), Map.of("statut", statut));
        return toResponse(validation);
    }

    /** Vérifie que l'entité référencée existe et appartient au tenant — seul rempart d'intégrité pour cette relation polymorphe. */
    private void verifierExistenceEtTenant(String entiteType, UUID entiteId, UUID entrepriseId) {
        boolean existe = switch (entiteType) {
            case "DOSSIER" -> { dossierService.findWithinTenant(entiteId); yield true; } // lève DOSSIER_NOT_FOUND sinon
            case "FACTURE" -> { factureService.findWithinTenant(entiteId); yield true; }
            case "COTATION" -> cotationRepository.findByIdAndEntrepriseId(entiteId, entrepriseId).isPresent();
            case "PAIEMENT" -> paiementRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(entiteId, entrepriseId).isPresent();
            case "CHARGE" -> chargeRepository.findByIdAndEntrepriseId(entiteId, entrepriseId).isPresent();
            default -> false;
        };
        if (!existe) {
            throw BusinessException.notFound(ErrorCode.VALIDATION_ERROR,
                    "Entité " + entiteType + " introuvable pour cette entreprise");
        }
    }

    private ValidationRequest findWithinTenant(UUID id) {
        return validationRepository.findByIdAndEntrepriseId(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.VALIDATION_NOT_FOUND, "Demande de validation introuvable"));
    }

    private ValidationResponse toResponse(ValidationRequest v) {
        return new ValidationResponse(v.getId(), v.getType(), v.getEntiteType(), v.getEntiteId(), v.getDemandeurId(),
                v.getValidateurId(), v.getStatut(), v.getCommentaire(), v.getDateDemande(), v.getDateDecision());
    }
}
