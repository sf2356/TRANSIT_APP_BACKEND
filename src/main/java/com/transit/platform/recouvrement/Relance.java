package com.transit.platform.recouvrement;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "relances")
public class Relance {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(name = "facture_id", nullable = false)
    private UUID factureId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "type_relance", nullable = false)
    private String typeRelance;

    @Column(nullable = false)
    private String statut = "A_RELANCER";

    @Column(name = "date_relance", nullable = false)
    private LocalDate dateRelance = LocalDate.now();

    @Column(name = "prochaine_relance")
    private LocalDate prochaineRelance;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public UUID getFactureId() { return factureId; }
    public void setFactureId(UUID factureId) { this.factureId = factureId; }
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public String getTypeRelance() { return typeRelance; }
    public void setTypeRelance(String typeRelance) { this.typeRelance = typeRelance; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDate getDateRelance() { return dateRelance; }
    public void setDateRelance(LocalDate dateRelance) { this.dateRelance = dateRelance; }
    public LocalDate getProchaineRelance() { return prochaineRelance; }
    public void setProchaineRelance(LocalDate prochaineRelance) { this.prochaineRelance = prochaineRelance; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
