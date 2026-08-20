package com.transit.platform.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    /** Rôles disponibles pour une entreprise : rôles système (partagés) + rôles propres. */
    @Query("SELECT r FROM Role r WHERE r.entrepriseId IS NULL OR r.entrepriseId = :entrepriseId")
    List<Role> findAvailableForEntreprise(UUID entrepriseId);

    /** Lecture stricte d'un rôle personnalisé de l'entreprise (jamais un rôle système d'une autre entreprise). */
    Optional<Role> findByIdAndEntrepriseId(UUID id, UUID entrepriseId);

    /** Lecture d'un rôle accessible (système OU propre à l'entreprise) — pour affichage seul, jamais pour modification. */
    @Query("SELECT r FROM Role r WHERE r.id = :id AND (r.entrepriseId IS NULL OR r.entrepriseId = :entrepriseId)")
    Optional<Role> findByIdAccessibleForEntreprise(UUID id, UUID entrepriseId);
}
