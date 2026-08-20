package com.transit.platform.caisse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MouvementCaisseRepository extends JpaRepository<MouvementCaisse, UUID> {

    Optional<MouvementCaisse> findByIdAndEntrepriseId(UUID id, UUID entrepriseId);

    @Query("SELECT m FROM MouvementCaisse m WHERE m.entrepriseId = :entrepriseId " +
           "AND (:typeMouvement IS NULL OR m.typeMouvement = :typeMouvement) " +
           "AND (:statut IS NULL OR m.statut = :statut) " +
            "AND (CAST(:dateDebut AS timestamp) IS NULL OR m.dateMouvement >= CAST(:dateDebut AS timestamp)) " +
            "AND (CAST(:dateFin AS timestamp) IS NULL OR m.dateMouvement <= CAST(:dateFin AS timestamp))")
    Page<MouvementCaisse> search(@Param("entrepriseId") UUID entrepriseId, @Param("typeMouvement") String typeMouvement,
                                  @Param("statut") String statut, @Param("dateDebut") Instant dateDebut,
                                  @Param("dateFin") Instant dateFin, Pageable pageable);

    /** Alimente GET /caisse/resume — le solde n'est JAMAIS stocké, toujours recalculé (Prompt 02 §21). */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM MouvementCaisse m " +
           "WHERE m.entrepriseId = :entrepriseId AND m.typeMouvement = :type AND m.statut = 'VALIDE'")
    java.math.BigDecimal sumByType(@Param("entrepriseId") UUID entrepriseId, @Param("type") String type);

    long countByEntrepriseIdAndStatut(UUID entrepriseId, String statut);

    @Query("SELECT COALESCE(SUM(m.montant),0) FROM MouvementCaisse m WHERE m.entrepriseId = :entrepriseId " +
           "AND m.typeMouvement = :type AND m.statut = 'VALIDE' " +
           "AND (:dossierId IS NULL OR m.dossierId = :dossierId) " +
            "AND (CAST(:dateDebut AS timestamp) IS NULL OR m.dateMouvement >= CAST(:dateDebut AS timestamp)) AND (CAST(:dateFin AS timestamp) IS NULL OR m.dateMouvement <= CAST(:dateFin AS timestamp))")
    java.math.BigDecimal sumByTypeFiltre(@Param("entrepriseId") UUID entrepriseId, @Param("type") String type,
                                          @Param("dossierId") UUID dossierId, @Param("dateDebut") Instant dateDebut,
                                          @Param("dateFin") Instant dateFin);
}
