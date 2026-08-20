package com.transit.platform.tiers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TiersRepository extends JpaRepository<Tiers, UUID> {

    Optional<Tiers> findByIdAndEntrepriseIdAndDeletedAtIsNull(UUID id, UUID entrepriseId);

    @Query("SELECT t FROM Tiers t WHERE t.entrepriseId = :entrepriseId AND t.deletedAt IS NULL " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:statut IS NULL OR t.statut = :statut) " +
           "AND (:search IS NULL OR LOWER(t.raisonSociale) LIKE %:search% OR LOWER(t.email) LIKE %:search% OR t.telephone LIKE %:search%)")
    Page<Tiers> search(@Param("entrepriseId") UUID entrepriseId, @Param("type") String type,
                        @Param("statut") String statut, @Param("search") String search, Pageable pageable);
}
