package com.transit.platform.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(nullable = false)
    private String action;

    @Column(name = "entite_type", nullable = false)
    private String entiteType;

    @Column(name = "entite_id", nullable = false)
    private UUID entiteId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancienne_valeur")
    private Map<String, Object> ancienneValeur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelle_valeur")
    private Map<String, Object> nouvelleValeur;

    @Column(name = "adresse_ip")
    private String adresseIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "date_action", nullable = false)
    private Instant dateAction = Instant.now();

    // Getters / setters
    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(UUID utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntiteType() { return entiteType; }
    public void setEntiteType(String entiteType) { this.entiteType = entiteType; }
    public UUID getEntiteId() { return entiteId; }
    public void setEntiteId(UUID entiteId) { this.entiteId = entiteId; }
    public Map<String, Object> getAncienneValeur() { return ancienneValeur; }
    public void setAncienneValeur(Map<String, Object> ancienneValeur) { this.ancienneValeur = ancienneValeur; }
    public Map<String, Object> getNouvelleValeur() { return nouvelleValeur; }
    public void setNouvelleValeur(Map<String, Object> nouvelleValeur) { this.nouvelleValeur = nouvelleValeur; }
    public String getAdresseIp() { return adresseIp; }
    public void setAdresseIp(String adresseIp) { this.adresseIp = adresseIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Instant getDateAction() { return dateAction; }
}
