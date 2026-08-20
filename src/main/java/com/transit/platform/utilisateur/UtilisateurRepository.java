package com.transit.platform.utilisateur;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByIdAndEntrepriseIdAndDeletedAtIsNull(UUID id, UUID entrepriseId);

    Optional<Utilisateur> findByEmailAndEntrepriseIdAndDeletedAtIsNull(String email, UUID entrepriseId);

    /**
     * Recherche par email SEUL au login : à ce stade l'entreprise n'est pas encore connue
     * (c'est justement ce que le login doit déterminer). Cas volontairement distinct du
     * schéma multi-tenant standard qui filtre toujours par entrepriseId.
     */
    Optional<Utilisateur> findByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT u FROM Utilisateur u WHERE u.entrepriseId = :entrepriseId AND u.deletedAt IS NULL " +
           "AND (:search IS NULL OR LOWER(u.nom) LIKE %:search% OR LOWER(u.prenom) LIKE %:search% OR LOWER(u.email) LIKE %:search%)")
    Page<Utilisateur> search(@Param("entrepriseId") UUID entrepriseId, @Param("search") String search, Pageable pageable);
}
