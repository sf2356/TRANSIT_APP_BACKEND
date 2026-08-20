package com.transit.platform.cotation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigneCotationRepository extends JpaRepository<LigneCotation, UUID> {
    List<LigneCotation> findByCotationIdOrderByOrdreAsc(UUID cotationId);
    Optional<LigneCotation> findByIdAndCotationId(UUID id, UUID cotationId);
    void deleteByCotationId(UUID cotationId);
}
