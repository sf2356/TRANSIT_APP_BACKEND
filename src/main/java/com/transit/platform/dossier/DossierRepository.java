package com.transit.platform.dossier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DossierRepository extends JpaRepository<Dossier, UUID> {

    /**
     * Toute lecture d'un dossier passe par cette méthode (id + entrepriseId) : c'est la
     * garantie structurelle contre l'IDOR inter-tenant — un dossier d'une autre entreprise
     * renvoie simplement "introuvable", jamais son contenu.
     */
    Optional<Dossier> findByIdAndEntrepriseIdAndDeletedAtIsNull(UUID id, UUID entrepriseId);

    @Query("SELECT d FROM Dossier d WHERE d.entrepriseId = :entrepriseId AND d.deletedAt IS NULL " +
           "AND (:statut IS NULL OR d.statut = :statut) " +
           "AND (:clientId IS NULL OR d.clientId = :clientId) " +
           "AND (:responsableId IS NULL OR d.responsableId = :responsableId) " +
           "AND (:search IS NULL OR LOWER(d.titre) LIKE %:search% OR LOWER(d.numero) LIKE %:search%)")
    Page<Dossier> search(@Param("entrepriseId") UUID entrepriseId, @Param("statut") String statut,
                          @Param("clientId") UUID clientId, @Param("responsableId") UUID responsableId,
                          @Param("search") String search, Pageable pageable);

    /** Projection pour le comptage de dossiers par statut (dashboard opérations/global). */
    interface StatutCount {
        String getStatut();
        long getTotal();
    }

    @Query("SELECT d.statut AS statut, COUNT(d) AS total FROM Dossier d " +
           "WHERE d.entrepriseId = :entrepriseId AND d.deletedAt IS NULL GROUP BY d.statut")
    List<StatutCount> countByStatut(@Param("entrepriseId") UUID entrepriseId);

    @Query("SELECT COUNT(d) FROM Dossier d WHERE d.entrepriseId = :entrepriseId AND d.deletedAt IS NULL " +
           "AND d.dateEcheance BETWEEN :debut AND :fin AND d.statut NOT IN ('CLOTURE','ANNULE')")
    long countProchesEcheance(@Param("entrepriseId") UUID entrepriseId, @Param("debut") java.time.LocalDate debut,
                               @Param("fin") java.time.LocalDate fin);
}
