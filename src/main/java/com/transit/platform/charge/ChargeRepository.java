package com.transit.platform.charge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {

    Optional<Charge> findByIdAndEntrepriseId(UUID id, UUID entrepriseId);

    List<Charge> findByDossierId(UUID dossierId);

    @Query("SELECT c FROM Charge c WHERE c.entrepriseId = :entrepriseId " +
           "AND (:dossierId IS NULL OR c.dossierId = :dossierId) " +
           "AND (:categorie IS NULL OR c.categorie = :categorie) " +
           "AND (:fournisseurId IS NULL OR c.fournisseurId = :fournisseurId) " +
           "AND (:statut IS NULL OR c.statut = :statut) " +
            "AND (CAST(:dateDebut AS date) IS NULL OR c.dateCharge >= CAST(:dateDebut AS date)) " +
            "AND (CAST(:dateFin AS date) IS NULL OR c.dateCharge <= CAST(:dateFin AS date))")
    Page<Charge> search(@Param("entrepriseId") UUID entrepriseId, @Param("dossierId") UUID dossierId,
                         @Param("categorie") String categorie, @Param("fournisseurId") UUID fournisseurId,
                         @Param("statut") String statut, @Param("dateDebut") LocalDate dateDebut,
                         @Param("dateFin") LocalDate dateFin, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.montant),0) FROM Charge c WHERE c.entrepriseId = :entrepriseId AND c.statut <> 'REJETEE'")
    java.math.BigDecimal sumByEntreprise(@Param("entrepriseId") UUID entrepriseId);

    @Query("SELECT COALESCE(SUM(c.montant),0) FROM Charge c WHERE c.dossierId = :dossierId AND c.statut <> 'REJETEE'")
    java.math.BigDecimal sumByDossier(@Param("dossierId") UUID dossierId);

    @Query("SELECT COALESCE(SUM(c.montant),0) FROM Charge c WHERE c.dossierId = :dossierId AND c.type = :type AND c.statut <> 'REJETEE'")
    java.math.BigDecimal sumByDossierAndType(@Param("dossierId") UUID dossierId, @Param("type") String type);

    @Query("SELECT COALESCE(SUM(c.montant),0) FROM Charge c WHERE c.entrepriseId = :entrepriseId AND c.statut <> 'REJETEE' " +
           "AND (:dossierId IS NULL OR c.dossierId = :dossierId) " +
            "AND (CAST(:dateDebut AS date) IS NULL OR c.dateCharge >= CAST(:dateDebut AS date)) AND (CAST(:dateFin AS date) IS NULL OR c.dateCharge <= CAST(:dateFin AS date))")
    java.math.BigDecimal sumFiltre(@Param("entrepriseId") UUID entrepriseId, @Param("dossierId") UUID dossierId,
                                    @Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin);
}
