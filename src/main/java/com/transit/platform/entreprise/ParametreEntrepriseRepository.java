package com.transit.platform.entreprise;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ParametreEntrepriseRepository extends JpaRepository<ParametreEntreprise, UUID> {
    Optional<ParametreEntreprise> findByEntrepriseId(UUID entrepriseId);
}
