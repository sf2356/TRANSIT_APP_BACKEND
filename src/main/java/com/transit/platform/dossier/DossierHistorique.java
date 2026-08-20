package com.transit.platform.dossier;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Timeline métier dédiée au dossier (distincte de audit_logs, transverse et technique).
 * Sert directement l'affichage de l'onglet "Historique" côté Angular/Flutter.
 */
@Entity
@Table(name = "dossier_historique")
public class DossierHistorique {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entreprise_id", nullable = false)
    private UUID entrepriseId;

    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(nullable = false)
    private String evenement;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "date_evenement", nullable = false)
    private Instant dateEvenement = Instant.now();

    public UUID getId() { return id; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public UUID getDossierId() { return dossierId; }
    public void setDossierId(UUID dossierId) { this.dossierId = dossierId; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(UUID utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getEvenement() { return evenement; }
    public void setEvenement(String evenement) { this.evenement = evenement; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Instant getDateEvenement() { return dateEvenement; }
}
