package com.transit.platform.audit;

import com.transit.platform.audit.dto.AuditLogResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.security.TenantContext;
import com.transit.platform.utilisateur.Utilisateur;
import com.transit.platform.utilisateur.UtilisateurRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Consultation transverse du journal d'audit — réservée par défaut au DIRECTEUR (permission AUDIT_READ, cf. V20). */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Journal d'audit — écriture exclusivement via AuditService, lecture seule ici")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantContext tenantContext;

    public AuditController(AuditLogRepository auditLogRepository, UtilisateurRepository utilisateurRepository,
                           TenantContext tenantContext) {
        this.auditLogRepository = auditLogRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Rechercher dans le journal d'audit (filtrable par type d'entité, utilisateur)")
    public PagedApiResponse<AuditLogResponse> search(@RequestParam(required = false) String entiteType,
                                                     @RequestParam(required = false) UUID utilisateurId,
                                                     Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.search(tenantContext.currentEntrepriseId(), entiteType, utilisateurId, pageable);

        // Un même utilisateur revient sur de nombreuses lignes d'audit — un seul aller-retour
        // base de données pour toute la page plutôt qu'une requête par ligne (Prompt 04 §49 :
        // même principe déjà appliqué pour le numéro de dossier sur Documents/Paiements).
        Set<UUID> utilisateurIds = page.getContent().stream()
                .map(AuditLog::getUtilisateurId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> nomsParUtilisateur = utilisateurRepository.findAllById(utilisateurIds).stream()
                .collect(Collectors.toMap(Utilisateur::getId, u -> (u.getPrenom() + " " + u.getNom()).trim()));

        return PagedApiResponse.of(page.map(a -> new AuditLogResponse(a.getId(), a.getUtilisateurId(),
                nomsParUtilisateur.getOrDefault(a.getUtilisateurId(), "Utilisateur supprimé"), a.getAction(), a.getEntiteType(),
                a.getEntiteId(), a.getAncienneValeur(), a.getNouvelleValeur(), a.getAdresseIp(), a.getDateAction())));
    }
}