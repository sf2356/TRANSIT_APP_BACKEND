package com.transit.platform.dossier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DossierHistoriqueRepository extends JpaRepository<DossierHistorique, UUID> {
    Page<DossierHistorique> findByEntrepriseIdAndDossierIdOrderByDateEvenementDesc(
            UUID entrepriseId, UUID dossierId, Pageable pageable);
}
