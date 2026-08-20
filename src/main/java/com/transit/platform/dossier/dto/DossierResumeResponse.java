package com.transit.platform.dossier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vue synthétique de la page de détail dossier (Prompt 04 §16 / §38) : agrège en un seul
 * appel ce qui nécessiterait sinon 4-5 requêtes séparées côté frontend (dossier, compteurs,
 * rentabilité). Client et responsable inclus en sous-objets minimalistes (id + libellé) —
 * pas les entités complètes, pour ne pas alourdir inutilement la réponse (Prompt 03 §37).
 */
public record DossierResumeResponse(
        UUID id, String numero, String titre, String statut, String priorite,
        ClientResume client, ResponsableResume responsable,
        LocalDate dateOuverture, LocalDate dateEcheance,
        long nombreMarchandises, long nombreDocuments, long nombreCotations, long nombreFactures,
        BigDecimal totalFacture, BigDecimal totalEncaisse, BigDecimal reste, BigDecimal charges,
        BigDecimal margeEstimee, String devise
) {
    public record ClientResume(UUID id, String raisonSociale) {}
    public record ResponsableResume(UUID id, String nomComplet) {}
}
