package com.transit.platform.marchandise;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MarchandiseRepository extends JpaRepository<Marchandise, UUID> {

    long countByDossierId(UUID dossierId);

    Optional<Marchandise> findByIdAndDossierId(UUID id, UUID dossierId);

    Page<Marchandise> findByDossierId(UUID dossierId, Pageable pageable);

    /**
     * Liste "toutes entreprises confondues" filtrée par un ensemble de dossiers déjà
     * validés comme appartenant au tenant courant — utilisée par GET /api/v1/marchandises
     * (recherche transverse), voir MarchandiseService.searchGlobal.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT m FROM Marchandise m WHERE m.dossierId IN " +
            "(SELECT d.id FROM com.transit.platform.dossier.Dossier d WHERE d.entrepriseId = :entrepriseId AND d.deletedAt IS NULL) " +
            "AND (:statut IS NULL OR m.statut = :statut) " +
            "AND (:search IS NULL OR LOWER(m.designation) LIKE %:search%)")
    Page<Marchandise> searchForEntreprise(java.util.UUID entrepriseId, String statut, String search, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            "SELECT m FROM Marchandise m WHERE m.dossierId IN " +
                    "(SELECT d.id FROM com.transit.platform.dossier.Dossier d WHERE d.clientId = :clientId AND d.entrepriseId = :entrepriseId AND d.deletedAt IS NULL) " +
                    "ORDER BY m.createdAt DESC")
    java.util.List<Marchandise> findByClientId(@org.springframework.data.repository.query.Param("clientId") UUID clientId,
                                               @org.springframework.data.repository.query.Param("entrepriseId") UUID entrepriseId);
}
