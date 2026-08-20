package com.transit.platform.document;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(name = "marchandise_id")
    private UUID marchandiseId;

    @Column(name = "facture_id")
    private UUID factureId;

    @Column(name = "cotation_id")
    private UUID cotationId;

    @Column(nullable = false)
    private String titre;

    @Column(name = "type_document", nullable = false)
    private String typeDocument;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "type_mime", nullable = false)
    private String typeMime;

    @Column(nullable = false)
    private long taille;

    @Column(nullable = false)
    private String statut = "ACTIF";

    @Column(name = "date_reception")
    private LocalDate dateReception;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "ajoute_par", nullable = false)
    private UUID ajoutePar;

    @Column(name = "date_ajout", nullable = false)
    private Instant dateAjout = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public UUID getDossierId() { return dossierId; }
    public void setDossierId(UUID dossierId) { this.dossierId = dossierId; }
    public UUID getMarchandiseId() { return marchandiseId; }
    public void setMarchandiseId(UUID marchandiseId) { this.marchandiseId = marchandiseId; }
    public UUID getFactureId() { return factureId; }
    public void setFactureId(UUID factureId) { this.factureId = factureId; }
    public UUID getCotationId() { return cotationId; }
    public void setCotationId(UUID cotationId) { this.cotationId = cotationId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getTypeDocument() { return typeDocument; }
    public void setTypeDocument(String typeDocument) { this.typeDocument = typeDocument; }
    public String getCheminFichier() { return cheminFichier; }
    public void setCheminFichier(String cheminFichier) { this.cheminFichier = cheminFichier; }
    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }
    public String getTypeMime() { return typeMime; }
    public void setTypeMime(String typeMime) { this.typeMime = typeMime; }
    public long getTaille() { return taille; }
    public void setTaille(long taille) { this.taille = taille; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDate getDateReception() { return dateReception; }
    public void setDateReception(LocalDate dateReception) { this.dateReception = dateReception; }
    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }
    public UUID getAjoutePar() { return ajoutePar; }
    public void setAjoutePar(UUID ajoutePar) { this.ajoutePar = ajoutePar; }
    public Instant getDateAjout() { return dateAjout; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
