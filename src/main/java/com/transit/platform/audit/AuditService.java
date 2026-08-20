package com.transit.platform.audit;

import com.transit.platform.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

/**
 * Point d'entrée UNIQUE pour journaliser une opération sensible — évite de dupliquer la
 * logique d'écriture d'audit dans chaque service métier (Dossier, Facture, Paiement...).
 *
 * Table append-only : aucune méthode d'update/delete n'est exposée ici, volontairement.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final TenantContext tenantContext;

    public AuditService(AuditLogRepository auditLogRepository, TenantContext tenantContext) {
        this.auditLogRepository = auditLogRepository;
        this.tenantContext = tenantContext;
    }

    /** Transaction indépendante : un audit ne doit jamais être perdu par un rollback métier ultérieur non lié. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entiteType, UUID entiteId,
                     Map<String, Object> ancienneValeur, Map<String, Object> nouvelleValeur) {
        AuditLog log = new AuditLog();
        log.setEntrepriseId(tenantContext.currentEntrepriseId());
        log.setUtilisateurId(tenantContext.currentUtilisateurId());
        log.setAction(action);
        log.setEntiteType(entiteType);
        log.setEntiteId(entiteId);
        log.setAncienneValeur(ancienneValeur);
        log.setNouvelleValeur(nouvelleValeur);
        log.setAdresseIp(currentRemoteAddress());
        auditLogRepository.save(log);
    }

    private String currentRemoteAddress() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest request = sra.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
