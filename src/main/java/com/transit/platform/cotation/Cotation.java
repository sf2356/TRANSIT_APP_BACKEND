package com.transit.platform.cotation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cotations")
public class Cotation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(nullable = false)
    private String numero;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "dossier_id")
    private UUID dossierId;

    private String titre;

    @Column(name = "date_cotation", nullable = false)
    private LocalDate dateCotation = LocalDate.now();

    @Column(name = "date_validite")
    private LocalDate dateValidite;

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

    // Pas de relation @OneToMany vers LigneCotation ici : les lignes sont chargées explicitement
    // via LigneCotationRepository dans CotationService, pour garder un contrôle total sur le
    // rechargement/recalcul (voir principe "pas de EAGER inutile", Prompt 03 §49).

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public UUID getDossierId() { return dossierId; }
    public void setDossierId(UUID dossierId) { this.dossierId = dossierId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public LocalDate getDateCotation() { return dateCotation; }
    public void setDateCotation(LocalDate dateCotation) { this.dateCotation = dateCotation; }
    public LocalDate getDateValidite() { return dateValidite; }
    public void setDateValidite(LocalDate dateValidite) { this.dateValidite = dateValidite; }
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
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
