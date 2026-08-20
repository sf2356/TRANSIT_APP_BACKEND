package com.transit.platform.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByEntrepriseIdAndEntiteTypeAndEntiteIdOrderByDateActionDesc(
            UUID entrepriseId, String entiteType, UUID entiteId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            "SELECT a FROM AuditLog a WHERE a.entrepriseId = :entrepriseId " +
            "AND (:entiteType IS NULL OR a.entiteType = :entiteType) " +
            "AND (:utilisateurId IS NULL OR a.utilisateurId = :utilisateurId) " +
            "ORDER BY a.dateAction DESC")
    Page<AuditLog> search(@org.springframework.data.repository.query.Param("entrepriseId") UUID entrepriseId,
                           @org.springframework.data.repository.query.Param("entiteType") String entiteType,
                           @org.springframework.data.repository.query.Param("utilisateurId") UUID utilisateurId,
                           Pageable pageable);
}
