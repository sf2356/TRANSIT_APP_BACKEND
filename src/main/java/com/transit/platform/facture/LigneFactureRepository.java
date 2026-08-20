package com.transit.platform.facture;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigneFactureRepository extends JpaRepository<LigneFacture, UUID> {
    List<LigneFacture> findByFactureIdOrderByOrdreAsc(UUID factureId);
    Optional<LigneFacture> findByIdAndFactureId(UUID id, UUID factureId);
    void deleteByFactureId(UUID factureId);
}
