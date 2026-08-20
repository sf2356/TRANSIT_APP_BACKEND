package com.transit.platform.paiement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaiementRepository extends JpaRepository<Paiement, UUID> {

    Optional<Paiement> findByIdAndEntrepriseIdAndDeletedAtIsNull(UUID id, UUID entrepriseId);

    List<Paiement> findByFactureIdAndDeletedAtIsNull(UUID factureId);

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.factureId = :factureId AND p.statut = 'VALIDE' AND p.deletedAt IS NULL")
    BigDecimal sumValidePourFacture(@Param("factureId") UUID factureId);

    @Query("SELECT p FROM Paiement p WHERE p.entrepriseId = :entrepriseId AND p.deletedAt IS NULL " +
           "AND (:factureId IS NULL OR p.factureId = :factureId) " +
           "AND (:dossierId IS NULL OR p.dossierId = :dossierId) " +
           "AND (:statut IS NULL OR p.statut = :statut)")
    Page<Paiement> search(@Param("entrepriseId") UUID entrepriseId, @Param("factureId") UUID factureId,
                           @Param("dossierId") UUID dossierId, @Param("statut") String statut, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.montant),0) FROM Paiement p WHERE p.entrepriseId = :entrepriseId AND p.statut = 'VALIDE' AND p.deletedAt IS NULL")
    BigDecimal sumValideByEntreprise(@Param("entrepriseId") UUID entrepriseId);

    @Query("SELECT COALESCE(SUM(p.montant),0) FROM Paiement p WHERE p.dossierId = :dossierId AND p.statut = 'VALIDE' AND p.deletedAt IS NULL")
    BigDecimal sumValideByDossier(@Param("dossierId") UUID dossierId);

    @Query("SELECT COALESCE(SUM(p.montant),0) FROM Paiement p WHERE p.entrepriseId = :entrepriseId AND p.statut = 'VALIDE' AND p.deletedAt IS NULL " +
           "AND (:clientId IS NULL OR p.clientId = :clientId) AND (:dossierId IS NULL OR p.dossierId = :dossierId) " +
            "AND (CAST(:dateDebut AS date) IS NULL OR p.datePaiement >= CAST(:dateDebut AS date)) AND (CAST(:dateFin AS date) IS NULL OR p.datePaiement <= CAST(:dateFin AS date))")
    BigDecimal sumValideFiltre(@Param("entrepriseId") UUID entrepriseId, @Param("clientId") UUID clientId,
                                @Param("dossierId") UUID dossierId, @Param("dateDebut") java.time.LocalDate dateDebut,
                                @Param("dateFin") java.time.LocalDate dateFin);
    @Query("SELECT p FROM Paiement p WHERE p.clientId = :clientId AND p.entrepriseId = :entrepriseId AND p.deletedAt IS NULL ORDER BY p.datePaiement DESC")
    java.util.List<Paiement> findByClientId(@Param("clientId") UUID clientId, @Param("entrepriseId") UUID entrepriseId);
}
