package com.transit.platform.facture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactureRepository extends JpaRepository<Facture, UUID> {

    Optional<Facture> findByIdAndEntrepriseIdAndDeletedAtIsNull(UUID id, UUID entrepriseId);

    @Query("SELECT f FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL " +
           "AND (:statut IS NULL OR f.statut = :statut) " +
           "AND (:clientId IS NULL OR f.clientId = :clientId) " +
           "AND (:dossierId IS NULL OR f.dossierId = :dossierId) " +
           "AND (:search IS NULL OR LOWER(f.numero) LIKE %:search% OR LOWER(f.titre) LIKE %:search%)")
    Page<Facture> search(@Param("entrepriseId") UUID entrepriseId, @Param("statut") String statut,
                          @Param("clientId") UUID clientId, @Param("dossierId") UUID dossierId,
                          @Param("search") String search, Pageable pageable);

    /**
     * CORRECTIF AUDIT (Prompt 07 §37/§65) : "EN_RETARD" n'est PAS une valeur physiquement
     * stockée dans facture.statut (le champ reste EMISE/PARTIELLEMENT_PAYEE en base — voir
     * FactureService.recalculerApresPaiement, qui ne connaît que BROUILLON/EMISE/
     * PARTIELLEMENT_PAYEE/PAYEE). "En retard" est un état DÉRIVÉ (échéance dépassée + reste
     * dû > 0), déjà utilisé par countFacturesEnRetard/sumMontantEnRetard pour le dashboard.
     * Avant ce correctif, filtrer GET /factures?statut=EN_RETARD renvoyait TOUJOURS une
     * liste vide (comparaison stricte f.statut = 'EN_RETARD', valeur qui n'existe jamais) —
     * alors que le référentiel et les modèles Angular/Flutter proposent cette valeur comme
     * un statut filtrable normal. Cette requête dédiée traduit le filtre "EN_RETARD" en la
     * même définition que le dashboard, sans toucher à la colonne statut ni au frontend.
     */
    @Query("SELECT f FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL " +
           "AND f.dateEcheance < CURRENT_DATE AND f.resteAPayer > 0 " +
           "AND (:clientId IS NULL OR f.clientId = :clientId) " +
           "AND (:dossierId IS NULL OR f.dossierId = :dossierId) " +
           "AND (:search IS NULL OR LOWER(f.numero) LIKE %:search% OR LOWER(f.titre) LIKE %:search%)")
    Page<Facture> searchEnRetard(@Param("entrepriseId") UUID entrepriseId, @Param("clientId") UUID clientId,
                                  @Param("dossierId") UUID dossierId, @Param("search") String search, Pageable pageable);

    Page<Facture> findByEntrepriseIdAndDossierIdAndDeletedAtIsNull(UUID entrepriseId, UUID dossierId, Pageable pageable);

    long countByEntrepriseIdAndDossierIdAndDeletedAtIsNull(UUID entrepriseId, UUID dossierId);

    // --- Agrégations dashboard / rentabilité (étape 20) ---

    @Query("SELECT COALESCE(SUM(f.montantTotal),0) FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL")
    java.math.BigDecimal sumMontantTotalByEntreprise(@Param("entrepriseId") UUID entrepriseId);

    @Query("SELECT COALESCE(SUM(f.resteAPayer),0) FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL")
    java.math.BigDecimal sumResteAPayerByEntreprise(@Param("entrepriseId") UUID entrepriseId);

    @Query("SELECT COALESCE(SUM(f.montantTotal),0) FROM Facture f WHERE f.dossierId = :dossierId AND f.deletedAt IS NULL")
    java.math.BigDecimal sumMontantTotalByDossier(@Param("dossierId") UUID dossierId);

    @Query("SELECT COUNT(f) FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL " +
           "AND f.dateEcheance < :today AND f.resteAPayer > 0")
    long countFacturesEnRetard(@Param("entrepriseId") UUID entrepriseId, @Param("today") java.time.LocalDate today);

    @Query("SELECT COALESCE(SUM(f.resteAPayer),0) FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL " +
           "AND f.dateEcheance < :today AND f.resteAPayer > 0")
    java.math.BigDecimal sumMontantEnRetard(@Param("entrepriseId") UUID entrepriseId, @Param("today") java.time.LocalDate today);

    /** Projection pour le classement des meilleurs clients (Prompt 03 §31). */
    interface ClientTotal {
        UUID getClientId();
        java.math.BigDecimal getTotal();
    }

    @Query("SELECT f.clientId AS clientId, SUM(f.montantTotal) AS total FROM Facture f " +
           "WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL GROUP BY f.clientId ORDER BY SUM(f.montantTotal) DESC")
    List<ClientTotal> topClients(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    /** Utilisé par ComptabiliteService (Prompt 03 §33) — mêmes filtres que la recherche standard, agrégés. */
    @Query("SELECT COALESCE(SUM(f.montantTotal),0) FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL " +
           "AND (:clientId IS NULL OR f.clientId = :clientId) AND (:dossierId IS NULL OR f.dossierId = :dossierId) " +
            "AND (CAST(:dateDebut AS date) IS NULL OR f.dateDocument >= CAST(:dateDebut AS date)) AND (CAST(:dateFin AS date) IS NULL OR f.dateDocument <= CAST(:dateFin AS date))")
    java.math.BigDecimal sumMontantTotalFiltre(@Param("entrepriseId") UUID entrepriseId, @Param("clientId") UUID clientId,
                                                @Param("dossierId") UUID dossierId, @Param("dateDebut") java.time.LocalDate dateDebut,
                                                @Param("dateFin") java.time.LocalDate dateFin);

    @Query("SELECT COALESCE(SUM(f.resteAPayer),0) FROM Facture f WHERE f.entrepriseId = :entrepriseId AND f.deletedAt IS NULL " +
           "AND (:clientId IS NULL OR f.clientId = :clientId) AND (:dossierId IS NULL OR f.dossierId = :dossierId) " +
            "AND (CAST(:dateDebut AS date) IS NULL OR f.dateDocument >= CAST(:dateDebut AS date)) AND (CAST(:dateFin AS date) IS NULL OR f.dateDocument <= CAST(:dateFin AS date))")
    java.math.BigDecimal sumResteAPayerFiltre(@Param("entrepriseId") UUID entrepriseId, @Param("clientId") UUID clientId,
                                               @Param("dossierId") UUID dossierId, @Param("dateDebut") java.time.LocalDate dateDebut,
                                               @Param("dateFin") java.time.LocalDate dateFin);
}
