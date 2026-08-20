package com.transit.platform.role;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue
    private UUID id;

    /** NULL = rôle système partagé (catalogue global) ; sinon rôle personnalisé de l'entreprise. */
    @Column(name = "entreprise_id")
    private UUID entrepriseId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "est_systeme", nullable = false)
    private boolean estSysteme;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public boolean isEstSysteme() { return estSysteme; }
    public void setEstSysteme(boolean estSysteme) { this.estSysteme = estSysteme; }
    public Set<Permission> getPermissions() { return permissions; }
}
