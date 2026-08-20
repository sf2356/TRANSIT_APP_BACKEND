package com.transit.platform.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, IdempotencyKeyEntity.Key> {
    Optional<IdempotencyKeyEntity> findByEntrepriseIdAndIdempotencyKeyAndEndpoint(UUID entrepriseId, String idempotencyKey, String endpoint);
}
