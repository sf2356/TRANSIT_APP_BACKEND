package com.transit.platform.facture;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "factures")
public class Facture {

    @Id
    @GeneratedValue
    private UUID id;

    /** Correctif audit (Prompt 07 §36) : verrouillage optimiste — voir migration V23. */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(nullable = false)
    private String numero;

    @Column(name = "type_document", nullable = false)
    private String typeDocument = "FACTURE";

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "dossier_id")
    private UUID dossierId;

    @Column(name = "cotation_id")
    private UUID cotationId;

    private String titre;

    @Column(name = "date_document", nullable = false)
    private LocalDate dateDocument = LocalDate.now();

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(nullable = false)
    private String devise;

    @Column(nullable = false)
    private String statut = "BROUILLON";

    @Column(name = "montant_ht", nullable = false)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_taxe", nullable = false)
    private BigDecimal montantTaxe = BigDecimal.ZERO;

    @Column(name = "montant_total", nullable = false)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "montant_paye", nullable = false)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "reste_a_payer", nullable = false)
    private BigDecimal resteAPayer = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getTypeDocument() { return typeDocument; }
    public void setTypeDocument(String typeDocument) { this.typeDocument = typeDocument; }
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public UUID getDossierId() { return dossierId; }
    public void setDossierId(UUID dossierId) { this.dossierId = dossierId; }
    public UUID getCotationId() { return cotationId; }
    public void setCotationId(UUID cotationId) { this.cotationId = cotationId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public LocalDate getDateDocument() { return dateDocument; }
    public void setDateDocument(LocalDate dateDocument) { this.dateDocument = dateDocument; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }
    public BigDecimal getMontantTaxe() { return montantTaxe; }
    public void setMontantTaxe(BigDecimal montantTaxe) { this.montantTaxe = montantTaxe; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }
    public BigDecimal getResteAPayer() { return resteAPayer; }
    public void setResteAPayer(BigDecimal resteAPayer) { this.resteAPayer = resteAPayer; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
