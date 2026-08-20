package com.transit.platform.validation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Nommée ValidationRequest (et non "Validation") pour éviter toute confusion avec jakarta.validation. */
@Entity
@Table(name = "validations")
public class ValidationRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(nullable = false)
    private String type;

    @Column(name = "entite_type", nullable = false)
    private String entiteType;

    @Column(name = "entite_id", nullable = false)
    private UUID entiteId;

    @Column(name = "demandeur_id", nullable = false)
    private UUID demandeurId;

    @Column(name = "validateur_id")
    private UUID validateurId;

    @Column(nullable = false)
    private String statut = "EN_ATTENTE";

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_demande", nullable = false)
    private Instant dateDemande = Instant.now();

    @Column(name = "date_decision")
    private Instant dateDecision;

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEntiteType() { return entiteType; }
    public void setEntiteType(String entiteType) { this.entiteType = entiteType; }
    public UUID getEntiteId() { return entiteId; }
    public void setEntiteId(UUID entiteId) { this.entiteId = entiteId; }
    public UUID getDemandeurId() { return demandeurId; }
    public void setDemandeurId(UUID demandeurId) { this.demandeurId = demandeurId; }
    public UUID getValidateurId() { return validateurId; }
    public void setValidateurId(UUID validateurId) { this.validateurId = validateurId; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public Instant getDateDemande() { return dateDemande; }
    public Instant getDateDecision() { return dateDecision; }
    public void setDateDecision(Instant dateDecision) { this.dateDecision = dateDecision; }
}
