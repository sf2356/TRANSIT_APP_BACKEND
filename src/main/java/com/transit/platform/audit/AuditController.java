package com.transit.platform.audit;

import com.transit.platform.audit.dto.AuditLogResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Consultation transverse du journal d'audit — réservée par défaut au DIRECTEUR (permission AUDIT_READ, cf. V20). */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Journal d'audit — écriture exclusivement via AuditService, lecture seule ici")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final TenantContext tenantContext;

    public AuditController(AuditLogRepository auditLogRepository, TenantContext tenantContext) {
        this.auditLogRepository = auditLogRepository;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Rechercher dans le journal d'audit (filtrable par type d'entité, utilisateur)")
    public PagedApiResponse<AuditLogResponse> search(@RequestParam(required = false) String entiteType,
                                                       @RequestParam(required = false) UUID utilisateurId,
                                                       Pageable pageable) {
        return PagedApiResponse.of(auditLogRepository.search(tenantContext.currentEntrepriseId(), entiteType, utilisateurId, pageable)
                .map(a -> new AuditLogResponse(a.getId(), a.getUtilisateurId(), a.getAction(), a.getEntiteType(),
                        a.getEntiteId(), a.getAncienneValeur(), a.getNouvelleValeur(), a.getAdresseIp(), a.getDateAction())));
    }
}
