package com.transit.platform.marchandise;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marchandises")
public class Marchandise {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(nullable = false)
    private String designation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "type_marchandise")
    private String typeMarchandise;

    @Column(nullable = false)
    private String statut = "DECLAREE";

    @Column(name = "nombre_colis")
    private Integer nombreColis;

    @Column(name = "type_colis")
    private String typeColis;

    @Column(name = "poids_brut")
    private BigDecimal poidsBrut;

    @Column(name = "volume_total")
    private BigDecimal volumeTotal;

    @Column(name = "numero_conteneur")
    private String numeroConteneur;

    @Column(name = "type_conteneur")
    private String typeConteneur;

    @Column(name = "document_transport")
    private String documentTransport;

    private String plomb;
    private String origine;
    private String destination;

    @Column(name = "nature_marchandise")
    private String natureMarchandise;

    @Column(name = "marque_reference")
    private String marqueReference;

    @Column(name = "valeur_declaree")
    private BigDecimal valeurDeclaree;

    @Column(name = "devise_valeur")
    private String deviseValeur;

    @Column(name = "code_sh")
    private String codeSH;

    @Column(name = "pays_origine")
    private String paysOrigine;

    @Column(name = "pays_provenance")
    private String paysProvenance;

    @Column(name = "destination_finale")
    private String destinationFinale;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "observations_douane", columnDefinition = "TEXT")
    private String observationsDouane;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Getters / setters
    public UUID getId() { return id; }
    public UUID getDossierId() { return dossierId; }
    public void setDossierId(UUID dossierId) { this.dossierId = dossierId; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTypeMarchandise() { return typeMarchandise; }
    public void setTypeMarchandise(String typeMarchandise) { this.typeMarchandise = typeMarchandise; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public Integer getNombreColis() { return nombreColis; }
    public void setNombreColis(Integer nombreColis) { this.nombreColis = nombreColis; }
    public String getTypeColis() { return typeColis; }
    public void setTypeColis(String typeColis) { this.typeColis = typeColis; }
    public BigDecimal getPoidsBrut() { return poidsBrut; }
    public void setPoidsBrut(BigDecimal poidsBrut) { this.poidsBrut = poidsBrut; }
    public BigDecimal getVolumeTotal() { return volumeTotal; }
    public void setVolumeTotal(BigDecimal volumeTotal) { this.volumeTotal = volumeTotal; }
    public String getNumeroConteneur() { return numeroConteneur; }
    public void setNumeroConteneur(String numeroConteneur) { this.numeroConteneur = numeroConteneur; }
    public String getTypeConteneur() { return typeConteneur; }
    public void setTypeConteneur(String typeConteneur) { this.typeConteneur = typeConteneur; }
    public String getDocumentTransport() { return documentTransport; }
    public void setDocumentTransport(String documentTransport) { this.documentTransport = documentTransport; }
    public String getPlomb() { return plomb; }
    public void setPlomb(String plomb) { this.plomb = plomb; }
    public String getOrigine() { return origine; }
    public void setOrigine(String origine) { this.origine = origine; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getNatureMarchandise() { return natureMarchandise; }
    public void setNatureMarchandise(String natureMarchandise) { this.natureMarchandise = natureMarchandise; }
    public String getMarqueReference() { return marqueReference; }
    public void setMarqueReference(String marqueReference) { this.marqueReference = marqueReference; }
    public BigDecimal getValeurDeclaree() { return valeurDeclaree; }
    public void setValeurDeclaree(BigDecimal valeurDeclaree) { this.valeurDeclaree = valeurDeclaree; }
    public String getDeviseValeur() { return deviseValeur; }
    public void setDeviseValeur(String deviseValeur) { this.deviseValeur = deviseValeur; }
    public String getCodeSH() { return codeSH; }
    public void setCodeSH(String codeSH) { this.codeSH = codeSH; }
    public String getPaysOrigine() { return paysOrigine; }
    public void setPaysOrigine(String paysOrigine) { this.paysOrigine = paysOrigine; }
    public String getPaysProvenance() { return paysProvenance; }
    public void setPaysProvenance(String paysProvenance) { this.paysProvenance = paysProvenance; }
    public String getDestinationFinale() { return destinationFinale; }
    public void setDestinationFinale(String destinationFinale) { this.destinationFinale = destinationFinale; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public String getObservationsDouane() { return observationsDouane; }
    public void setObservationsDouane(String observationsDouane) { this.observationsDouane = observationsDouane; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
