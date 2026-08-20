package com.transit.platform.cotation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lignes_cotation")
public class LigneCotation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "cotation_id", nullable = false)
    private UUID cotationId;

    @Column(name = "categorie_frais")
    private String categorieFrais;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal quantite = BigDecimal.ONE;

    @Column(name = "prix_unitaire", nullable = false)
    private BigDecimal prixUnitaire;

    /** Toujours recalculé côté backend (quantite × prixUnitaire) — jamais accepté tel quel depuis le client. */
    @Column(nullable = false)
    private BigDecimal montant = BigDecimal.ZERO;

    @Column(name = "taux_taxe", nullable = false)
    private BigDecimal tauxTaxe = BigDecimal.ZERO;

    @Column(name = "montant_taxe", nullable = false)
    private BigDecimal montantTaxe = BigDecimal.ZERO;

    @Column(nullable = false)
    private int ordre = 0;

    public UUID getId() { return id; }
    public UUID getCotationId() { return cotationId; }
    public void setCotationId(UUID cotationId) { this.cotationId = cotationId; }
    public String getCategorieFrais() { return categorieFrais; }
    public void setCategorieFrais(String categorieFrais) { this.categorieFrais = categorieFrais; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public BigDecimal getTauxTaxe() { return tauxTaxe; }
    public void setTauxTaxe(BigDecimal tauxTaxe) { this.tauxTaxe = tauxTaxe; }
    public BigDecimal getMontantTaxe() { return montantTaxe; }
    public void setMontantTaxe(BigDecimal montantTaxe) { this.montantTaxe = montantTaxe; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
}
