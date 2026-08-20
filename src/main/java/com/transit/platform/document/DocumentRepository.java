package com.transit.platform.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndEntrepriseIdAndDeletedAtIsNull(UUID id, UUID entrepriseId);

    Page<Document> findByDossierIdAndEntrepriseIdAndDeletedAtIsNull(UUID dossierId, UUID entrepriseId, Pageable pageable);

    long countByDossierIdAndEntrepriseIdAndDeletedAtIsNull(UUID dossierId, UUID entrepriseId);
    @org.springframework.data.jpa.repository.Query(
            "SELECT doc FROM Document doc WHERE doc.dossierId IN " +
                    "(SELECT d.id FROM com.transit.platform.dossier.Dossier d WHERE d.clientId = :clientId AND d.entrepriseId = :entrepriseId AND d.deletedAt IS NULL) " +
                    "AND doc.entrepriseId = :entrepriseId AND doc.deletedAt IS NULL ORDER BY doc.dateAjout DESC")
    java.util.List<Document> findByClientId(@org.springframework.data.repository.query.Param("clientId") UUID clientId,
                                            @org.springframework.data.repository.query.Param("entrepriseId") UUID entrepriseId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT d FROM Document d WHERE d.entrepriseId = :entrepriseId AND d.deletedAt IS NULL " +
                    "AND (CAST(:dossierId AS java.util.UUID) IS NULL OR d.dossierId = :dossierId) " +
                    "AND (CAST(:typeDocument AS string) IS NULL OR d.typeDocument = :typeDocument) " +
                    "AND (CAST(:search AS string) IS NULL OR LOWER(d.titre) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
                    "ORDER BY d.dateAjout DESC")
    Page<Document> search(@org.springframework.data.repository.query.Param("entrepriseId") UUID entrepriseId,
                          @org.springframework.data.repository.query.Param("dossierId") UUID dossierId,
                          @org.springframework.data.repository.query.Param("typeDocument") String typeDocument,
                          @org.springframework.data.repository.query.Param("search") String search,
                          Pageable pageable);
}
