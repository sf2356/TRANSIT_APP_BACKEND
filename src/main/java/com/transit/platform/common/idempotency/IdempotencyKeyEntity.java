package com.transit.platform.common.idempotency;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyKeyEntity.Key.class)
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "entreprise_id")
    private UUID entrepriseId;

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Id
    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    /** Clé composite JPA — reflète la PK composite (entreprise_id, idempotency_key, endpoint). */
    public static class Key implements java.io.Serializable {
        private UUID entrepriseId;
        private String idempotencyKey;
        private String endpoint;

        public Key() {}

        public Key(UUID entrepriseId, String idempotencyKey, String endpoint) {
            this.entrepriseId = entrepriseId;
            this.idempotencyKey = idempotencyKey;
            this.endpoint = endpoint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return entrepriseId.equals(key.entrepriseId) && idempotencyKey.equals(key.idempotencyKey) && endpoint.equals(key.endpoint);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(entrepriseId, idempotencyKey, endpoint);
        }
    }
}
