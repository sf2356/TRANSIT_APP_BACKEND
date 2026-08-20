package com.transit.platform.entreprise;

import com.transit.platform.common.BaseAuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "entreprises")
public class Entreprise extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    private String telephone;
    private String adresse;
    private String pays;
    private String ville;

    @Column(name = "secteur_activite")
    private String secteurActivite;

    @Column(name = "devise_defaut", nullable = false)
    private String deviseDefaut;

    private String logo;

    private String rccm;
    private String ifu;

    @Column(name = "site_web")
    private String siteWeb;

    private String banque;
    private String iban;
    private String cachet;

    @Column(name = "mentions_legales", columnDefinition = "TEXT")
    private String mentionsLegales;

    @Column(name = "template_pdf")
    private String templatePdf = "MODERNE";

    @Column(name = "couleur_accent")
    private String couleurAccent = "#1E3A5F";

    @Column(name = "type_activite")
    private String typeActivite;

    @Column(nullable = false)
    private String statut;

    // Getters / setters
    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getSecteurActivite() { return secteurActivite; }
    public void setSecteurActivite(String secteurActivite) { this.secteurActivite = secteurActivite; }
    public String getDeviseDefaut() { return deviseDefaut; }
    public void setDeviseDefaut(String deviseDefaut) { this.deviseDefaut = deviseDefaut; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getRccm() { return rccm; }
    public void setRccm(String rccm) { this.rccm = rccm; }
    public String getIfu() { return ifu; }
    public void setIfu(String ifu) { this.ifu = ifu; }
    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }
    public String getBanque() { return banque; }
    public void setBanque(String banque) { this.banque = banque; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getCachet() { return cachet; }
    public void setCachet(String cachet) { this.cachet = cachet; }
    public String getMentionsLegales() { return mentionsLegales; }
    public String getTemplatePdf() { return templatePdf; }
    public void setTemplatePdf(String templatePdf) { this.templatePdf = templatePdf; }
    public String getCouleurAccent() { return couleurAccent; }
    public void setCouleurAccent(String couleurAccent) { this.couleurAccent = couleurAccent; }
    public void setMentionsLegales(String mentionsLegales) { this.mentionsLegales = mentionsLegales; }
    public String getTypeActivite() { return typeActivite; }
    public void setTypeActivite(String typeActivite) { this.typeActivite = typeActivite; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
