package com.transit.platform.recouvrement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RelanceRepository extends JpaRepository<Relance, UUID> {

    Optional<Relance> findByIdAndEntrepriseId(UUID id, UUID entrepriseId);

    Page<Relance> findByEntrepriseIdAndFactureIdOrderByDateRelanceDesc(UUID entrepriseId, UUID factureId, Pageable pageable);

    @Query("SELECT r FROM Relance r WHERE r.entrepriseId = :entrepriseId " +
           "AND (:clientId IS NULL OR r.clientId = :clientId) " +
           "AND (:statut IS NULL OR r.statut = :statut) " +
            "AND (CAST(:prochaineRelanceAvant AS date) IS NULL OR r.prochaineRelance <= CAST(:prochaineRelanceAvant AS date)) " +
            "AND (:statutFacture IS NULL OR r.factureId IN " +
            "     (SELECT f.id FROM com.transit.platform.facture.Facture f WHERE f.statut = :statutFacture)) " +
            "AND (CAST(:echeanceAvant AS date) IS NULL OR r.factureId IN " +
            "     (SELECT f.id FROM com.transit.platform.facture.Facture f WHERE f.dateEcheance <= CAST(:echeanceAvant AS date)))")
    Page<Relance> search(@Param("entrepriseId") UUID entrepriseId, @Param("clientId") UUID clientId,
                          @Param("statut") String statut, @Param("prochaineRelanceAvant") LocalDate prochaineRelanceAvant,
                          @Param("statutFacture") String statutFacture, @Param("echeanceAvant") LocalDate echeanceAvant,
                          Pageable pageable);

    interface StatutCount {
        String getStatut();
        long getTotal();
    }

    @Query("SELECT r.statut AS statut, COUNT(r) AS total FROM Relance r WHERE r.entrepriseId = :entrepriseId GROUP BY r.statut")
    java.util.List<StatutCount> countByStatut(@Param("entrepriseId") UUID entrepriseId);
}
