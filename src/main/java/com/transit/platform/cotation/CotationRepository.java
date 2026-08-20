package com.transit.platform.cotation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CotationRepository extends JpaRepository<Cotation, UUID> {

    Optional<Cotation> findByIdAndEntrepriseId(UUID id, UUID entrepriseId);

    @Query("SELECT c FROM Cotation c WHERE c.entrepriseId = :entrepriseId " +
           "AND (:statut IS NULL OR c.statut = :statut) " +
           "AND (:clientId IS NULL OR c.clientId = :clientId) " +
           "AND (:dossierId IS NULL OR c.dossierId = :dossierId) " +
           "AND (:search IS NULL OR LOWER(c.numero) LIKE %:search% OR LOWER(c.titre) LIKE %:search%)")
    Page<Cotation> search(@Param("entrepriseId") UUID entrepriseId, @Param("statut") String statut,
                           @Param("clientId") UUID clientId, @Param("dossierId") UUID dossierId,
                           @Param("search") String search, Pageable pageable);

    Page<Cotation> findByEntrepriseIdAndDossierId(UUID entrepriseId, UUID dossierId, Pageable pageable);

    long countByEntrepriseIdAndDossierId(UUID entrepriseId, UUID dossierId);
}
