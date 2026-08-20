package com.transit.platform.entreprise;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "parametres_entreprise")
public class ParametreEntreprise {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entreprise_id", nullable = false, unique = true)
    private UUID entrepriseId;

    @Column(name = "prefixe_dossier", nullable = false)
    private String prefixeDossier = "DOS";

    @Column(name = "prefixe_cotation", nullable = false)
    private String prefixeCotation = "COT";

    @Column(name = "prefixe_facture", nullable = false)
    private String prefixeFacture = "FAC";

    @Column(name = "prefixe_paiement", nullable = false)
    private String prefixePaiement = "PAY";

    private String logo;

    @Column(name = "signature_image")
    private String signatureImage;

    @Column(name = "nom_signataire")
    private String nomSignataire;

    @Column(name = "fonction_signataire")
    private String fonctionSignataire;

    @Column(nullable = false)
    private String devise;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_metier", nullable = false)
    private Map<String, Object> configMetier;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getPrefixeDossier() { return prefixeDossier; }
    public void setPrefixeDossier(String prefixeDossier) { this.prefixeDossier = prefixeDossier; }
    public String getPrefixeCotation() { return prefixeCotation; }
    public void setPrefixeCotation(String prefixeCotation) { this.prefixeCotation = prefixeCotation; }
    public String getPrefixeFacture() { return prefixeFacture; }
    public void setPrefixeFacture(String prefixeFacture) { this.prefixeFacture = prefixeFacture; }
    public String getPrefixePaiement() { return prefixePaiement; }
    public void setPrefixePaiement(String prefixePaiement) { this.prefixePaiement = prefixePaiement; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getSignatureImage() { return signatureImage; }
    public void setSignatureImage(String signatureImage) { this.signatureImage = signatureImage; }
    public String getNomSignataire() { return nomSignataire; }
    public void setNomSignataire(String nomSignataire) { this.nomSignataire = nomSignataire; }
    public String getFonctionSignataire() { return fonctionSignataire; }
    public void setFonctionSignataire(String fonctionSignataire) { this.fonctionSignataire = fonctionSignataire; }
    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }
    public Map<String, Object> getConfigMetier() { return configMetier; }
    public void setConfigMetier(Map<String, Object> configMetier) { this.configMetier = configMetier; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
