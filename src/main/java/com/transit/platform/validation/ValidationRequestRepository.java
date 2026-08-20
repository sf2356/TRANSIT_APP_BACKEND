package com.transit.platform.validation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ValidationRequestRepository extends JpaRepository<ValidationRequest, UUID> {

    Optional<ValidationRequest> findByIdAndEntrepriseId(UUID id, UUID entrepriseId);

    @Query("SELECT v FROM ValidationRequest v WHERE v.entrepriseId = :entrepriseId " +
           "AND (:statut IS NULL OR v.statut = :statut) " +
           "AND (:entiteType IS NULL OR v.entiteType = :entiteType) " +
           "AND (:demandeurId IS NULL OR v.demandeurId = :demandeurId)")
    Page<ValidationRequest> search(@Param("entrepriseId") UUID entrepriseId, @Param("statut") String statut,
                                    @Param("entiteType") String entiteType, @Param("demandeurId") UUID demandeurId,
                                    Pageable pageable);
}
