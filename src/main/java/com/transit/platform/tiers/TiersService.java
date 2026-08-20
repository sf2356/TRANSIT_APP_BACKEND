package com.transit.platform.tiers;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class TiersService {

    private final TiersRepository tiersRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public TiersService(TiersRepository tiersRepository, TenantContext tenantContext, AuditService auditService) {
        this.tiersRepository = tiersRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<TiersResponse> search(String type, String statut, String search, Pageable pageable) {
        String normalized = search == null ? null : "%" + search.toLowerCase() + "%";
        return tiersRepository.search(tenantContext.currentEntrepriseId(), type, statut, normalized, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TiersResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional
    public TiersResponse create(CreateTiersRequest request) {
        Tiers tiers = new Tiers();
        tiers.setEntrepriseId(tenantContext.currentEntrepriseId());
        tiers.setRaisonSociale(request.raisonSociale());
        tiers.setNomContact(request.nomContact());
        tiers.setType(request.type());
        tiers.setTelephone(request.telephone());
        tiers.setEmail(request.email());
        tiers.setAdresse(request.adresse());
        tiers.setVille(request.ville());
        tiers.setPays(request.pays());
        tiers.setIdentifiantFiscal(request.identifiantFiscal());
        tiers.setRegistreCommerce(request.registreCommerce());
        tiers.setBoitePostale(request.boitePostale());
        tiers.setNotes(request.notes());
        tiers.setStatut("ACTIF");
        tiers = tiersRepository.save(tiers);

        auditService.log("CREATE", "TIERS", tiers.getId(), null, Map.of("raisonSociale", tiers.getRaisonSociale()));
        return toResponse(tiers);
    }

    @Transactional
    public TiersResponse update(UUID id, UpdateTiersRequest request) {
        Tiers tiers = findWithinTenant(id);
        tiers.setRaisonSociale(request.raisonSociale());
        tiers.setNomContact(request.nomContact());
        tiers.setTelephone(request.telephone());
        tiers.setEmail(request.email());
        tiers.setAdresse(request.adresse());
        tiers.setVille(request.ville());
        tiers.setPays(request.pays());
        tiers.setIdentifiantFiscal(request.identifiantFiscal());
        tiers.setRegistreCommerce(request.registreCommerce());
        tiers.setNotes(request.notes());
        return toResponse(tiersRepository.save(tiers));
    }

    @Transactional
    public void suspend(UUID id) {
        Tiers tiers = findWithinTenant(id);
        tiers.setStatut("SUSPENDU");
        tiersRepository.save(tiers);
    }

    @Transactional
    public void activate(UUID id) {
        Tiers tiers = findWithinTenant(id);
        tiers.setStatut("ACTIF");
        tiersRepository.save(tiers);
    }

    private Tiers findWithinTenant(UUID id) {
        return tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.TIERS_NOT_FOUND, "Tiers introuvable"));
    }

    private TiersResponse toResponse(Tiers t) {
        return new TiersResponse(t.getId(), t.getRaisonSociale(), t.getNomContact(), t.getType(), t.getTelephone(),
                t.getEmail(), t.getAdresse(), t.getVille(), t.getPays(), t.getIdentifiantFiscal(),
                t.getRegistreCommerce(), t.getBoitePostale(), t.getStatut(), t.getNotes());
    }
}
