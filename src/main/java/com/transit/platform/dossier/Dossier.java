package com.transit.platform.dossier;

import com.transit.platform.common.BaseAuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dossiers")
public class Dossier extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    /** Correctif audit (Prompt 07 §36) : verrouillage optimiste — voir migration V23. */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(nullable = false, unique = false)
    private String numero;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(nullable = false)
    private String titre;

    @Column(name = "mode_transport")
    private String modeTransport;

    @Column(nullable = false)
    private String priorite = "NORMALE";

    @Column(name = "responsable_id")
    private UUID responsableId;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDate dateOuverture = LocalDate.now();

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @Column(nullable = false)
    private String statut = "BROUILLON";

    @Column(name = "numero_ordre_transit")
    private String numeroOrdreTransit;

    @Column(name = "date_ordre_transit")
    private LocalDate dateOrdreTransit;

    @Column(name = "reference_client")
    private String referenceClient;

    @Column(name = "donneur_ordre")
    private String donneurOrdre;

    @Column(name = "type_operation")
    private String typeOperation;

    @Column(name = "regime_douanier")
    private String regimeDouanier;

    private String incoterm;
    private String origine;
    private String provenance;
    private String destination;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Getters / setters
    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getModeTransport() { return modeTransport; }
    public void setModeTransport(String modeTransport) { this.modeTransport = modeTransport; }
    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }
    public UUID getResponsableId() { return responsableId; }
    public void setResponsableId(UUID responsableId) { this.responsableId = responsableId; }
    public LocalDate getDateOuverture() { return dateOuverture; }
    public void setDateOuverture(LocalDate dateOuverture) { this.dateOuverture = dateOuverture; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public LocalDate getDateCloture() { return dateCloture; }
    public void setDateCloture(LocalDate dateCloture) { this.dateCloture = dateCloture; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getNumeroOrdreTransit() { return numeroOrdreTransit; }
    public void setNumeroOrdreTransit(String numeroOrdreTransit) { this.numeroOrdreTransit = numeroOrdreTransit; }
    public LocalDate getDateOrdreTransit() { return dateOrdreTransit; }
    public void setDateOrdreTransit(LocalDate dateOrdreTransit) { this.dateOrdreTransit = dateOrdreTransit; }
    public String getReferenceClient() { return referenceClient; }
    public void setReferenceClient(String referenceClient) { this.referenceClient = referenceClient; }
    public String getDonneurOrdre() { return donneurOrdre; }
    public void setDonneurOrdre(String donneurOrdre) { this.donneurOrdre = donneurOrdre; }
    public String getTypeOperation() { return typeOperation; }
    public void setTypeOperation(String typeOperation) { this.typeOperation = typeOperation; }
    public String getRegimeDouanier() { return regimeDouanier; }
    public void setRegimeDouanier(String regimeDouanier) { this.regimeDouanier = regimeDouanier; }
    public String getIncoterm() { return incoterm; }
    public void setIncoterm(String incoterm) { this.incoterm = incoterm; }
    public String getOrigine() { return origine; }
    public void setOrigine(String origine) { this.origine = origine; }
    public String getProvenance() { return provenance; }
    public void setProvenance(String provenance) { this.provenance = provenance; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
