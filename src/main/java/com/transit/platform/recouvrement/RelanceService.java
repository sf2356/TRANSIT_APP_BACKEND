package com.transit.platform.recouvrement;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.facture.Facture;
import com.transit.platform.facture.FactureService;
import com.transit.platform.recouvrement.dto.*;
import com.transit.platform.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class RelanceService {

    private final RelanceRepository relanceRepository;
    private final FactureService factureService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public RelanceService(RelanceRepository relanceRepository, FactureService factureService,
                           TenantContext tenantContext, AuditService auditService) {
        this.relanceRepository = relanceRepository;
        this.factureService = factureService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<RelanceResponse> search(UUID clientId, String statut, String statutFacture,
                                        LocalDate echeanceAvant, LocalDate prochaineRelanceAvant, Pageable pageable) {
        LocalDate echeanceAvantEffective = echeanceAvant != null ? echeanceAvant : LocalDate.of(2100, 12, 31);
        LocalDate prochaineRelanceAvantEffective = prochaineRelanceAvant != null ? prochaineRelanceAvant : LocalDate.of(2100, 12, 31);
        return relanceRepository.search(tenantContext.currentEntrepriseId(), clientId, statut,
                prochaineRelanceAvantEffective, statutFacture, echeanceAvantEffective, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RelanceResponse> listByFacture(UUID factureId, Pageable pageable) {
        factureService.findWithinTenant(factureId);
        return relanceRepository.findByEntrepriseIdAndFactureIdOrderByDateRelanceDesc(
                tenantContext.currentEntrepriseId(), factureId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RelanceResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional
    public RelanceResponse create(CreateRelanceRequest request) {
        // La facture doit exister et appartenir au tenant — le client de la relance est
        // TOUJOURS déduit de la facture, jamais ressaisi (cohérent avec le principe de
        // simplification des workflows, Prompt 01 §9).
        Facture facture = factureService.findWithinTenant(request.factureId());

        Relance relance = new Relance();
        relance.setEntrepriseId(tenantContext.currentEntrepriseId());
        relance.setFactureId(facture.getId());
        relance.setClientId(facture.getClientId());
        relance.setTypeRelance(request.typeRelance());
        relance.setDateRelance(request.dateRelance() != null ? request.dateRelance() : LocalDate.now());
        relance.setProchaineRelance(request.prochaineRelance());
        relance.setCommentaire(request.commentaire());
        relance.setStatut("RELANCE");
        relance.setCreatedBy(tenantContext.currentUtilisateurId());
        relance = relanceRepository.save(relance);

        auditService.log("CREATE", "RELANCE", relance.getId(), null,
                Map.of("factureId", facture.getId().toString(), "typeRelance", relance.getTypeRelance()));
        return toResponse(relance);
    }

    @Transactional
    public RelanceResponse update(UUID id, UpdateRelanceRequest request) {
        Relance relance = findWithinTenant(id);
        relance.setStatut(request.statut());
        relance.setProchaineRelance(request.prochaineRelance());
        relance.setCommentaire(request.commentaire());
        relance.setUpdatedAt(java.time.Instant.now());
        return toResponse(relanceRepository.save(relance));
    }

    private Relance findWithinTenant(UUID id) {
        return relanceRepository.findByIdAndEntrepriseId(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.RELANCE_NOT_FOUND, "Relance introuvable"));
    }

    private RelanceResponse toResponse(Relance r) {
        return new RelanceResponse(r.getId(), r.getFactureId(), r.getClientId(), r.getTypeRelance(), r.getStatut(),
                r.getDateRelance(), r.getProchaineRelance(), r.getCommentaire());
    }
}
