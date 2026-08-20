package com.transit.platform.caisse;

import com.transit.platform.audit.AuditService;
import com.transit.platform.caisse.dto.*;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Le solde n'est jamais stocké : toujours recalculé par agrégation des mouvements VALIDE
 * (Prompt 02 §21, Prompt 03 §28). Voir aussi creerDepuisPaiement(), appelé exclusivement
 * par PaiementService — c'est le seul point où un mouvement de caisse est généré
 * automatiquement plutôt que saisi manuellement (Prompt 03 §53).
 */
@Service
public class CaisseService {

    private final MouvementCaisseRepository mouvementRepository;
    private final DossierService dossierService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public CaisseService(MouvementCaisseRepository mouvementRepository, DossierService dossierService,
                          TenantContext tenantContext, AuditService auditService) {
        this.mouvementRepository = mouvementRepository;
        this.dossierService = dossierService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<MouvementCaisseResponse> search(String typeMouvement, String statut, Instant dateDebut, Instant dateFin, Pageable pageable) {
        return mouvementRepository.search(tenantContext.currentEntrepriseId(), typeMouvement, statut,
                        borneDebutEffective(dateDebut), borneFinEffective(dateFin), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MouvementCaisseResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional
    public MouvementCaisseResponse create(CreateMouvementCaisseRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        if (request.dossierId() != null) {
            dossierService.findWithinTenant(request.dossierId());
        }
        if (!"ENTREE".equals(request.typeMouvement()) && !"SORTIE".equals(request.typeMouvement())) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR, "typeMouvement doit être ENTREE ou SORTIE");
        }

        MouvementCaisse mouvement = new MouvementCaisse();
        mouvement.setEntrepriseId(entrepriseId);
        mouvement.setDossierId(request.dossierId());
        mouvement.setTypeMouvement(request.typeMouvement());
        mouvement.setCategorie(request.categorie());
        mouvement.setLibelle(request.libelle());
        mouvement.setMontant(request.montant());
        mouvement.setDevise(request.devise() != null ? request.devise() : "XOF");
        mouvement.setModePaiement(request.modePaiement());
        mouvement.setReference(request.reference());
        mouvement.setNotes(request.notes());
        mouvement.setCreatedBy(tenantContext.currentUtilisateurId());
        mouvement = mouvementRepository.save(mouvement);

        auditService.log("CREATE", "MOUVEMENT_CAISSE", mouvement.getId(), null,
                Map.of("typeMouvement", mouvement.getTypeMouvement(), "montant", mouvement.getMontant()));
        return toResponse(mouvement);
    }

    @Transactional
    public MouvementCaisseResponse update(UUID id, UpdateMouvementCaisseRequest request) {
        MouvementCaisse mouvement = findWithinTenant(id);
        if (mouvement.getPaiementId() != null) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Un mouvement généré automatiquement depuis un paiement ne peut pas être modifié manuellement");
        }
        mouvement.setCategorie(request.categorie());
        mouvement.setLibelle(request.libelle());
        mouvement.setMontant(request.montant());
        mouvement.setReference(request.reference());
        mouvement.setNotes(request.notes());
        mouvement.setUpdatedAt(Instant.now());
        return toResponse(mouvementRepository.save(mouvement));
    }

    @Transactional
    public void annuler(UUID id) {
        MouvementCaisse mouvement = findWithinTenant(id);
        mouvement.setStatut("ANNULE");
        mouvement.setUpdatedAt(Instant.now());
        mouvementRepository.save(mouvement);
        auditService.log("CANCEL", "MOUVEMENT_CAISSE", mouvement.getId(), null, Map.of("statut", "ANNULE"));
    }

    @Transactional(readOnly = true)
    public CaisseResumeResponse resume() {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        BigDecimal totalEntrees = mouvementRepository.sumByType(entrepriseId, "ENTREE");
        BigDecimal totalSorties = mouvementRepository.sumByType(entrepriseId, "SORTIE");
        long enAttente = mouvementRepository.countByEntrepriseIdAndStatut(entrepriseId, "EN_ATTENTE");
        long total = mouvementRepository.search(entrepriseId, null, null, borneDebutEffective(null), borneFinEffective(null), Pageable.unpaged()).getTotalElements();
        return new CaisseResumeResponse(totalEntrees, totalSorties, totalEntrees.subtract(totalSorties), enAttente, total);
    }

    /**
     * Point d'entrée unique pour la génération automatique d'un mouvement de caisse depuis
     * un paiement (Prompt 03 §53). Appelé exclusivement par PaiementService — jamais par un
     * contrôleur. Règle retenue (à valider avec le métier, voir README) : seul un paiement
     * en ESPECES génère automatiquement une entrée de caisse ; les autres modes (virement,
     * chèque, mobile money, carte) transitent par une banque et ne mouvementent pas la
     * caisse physique.
     */
    @Transactional
    public void creerDepuisPaiement(UUID entrepriseId, UUID dossierId, UUID paiementId, BigDecimal montant,
                                     String devise, String modePaiement, String libelle, UUID createdBy) {
        MouvementCaisse mouvement = new MouvementCaisse();
        mouvement.setEntrepriseId(entrepriseId);
        mouvement.setDossierId(dossierId);
        mouvement.setPaiementId(paiementId);
        mouvement.setTypeMouvement("ENTREE");
        mouvement.setCategorie("PAIEMENT_CLIENT");
        mouvement.setLibelle(libelle);
        mouvement.setMontant(montant);
        mouvement.setDevise(devise);
        mouvement.setModePaiement(modePaiement);
        mouvement.setCreatedBy(createdBy);
        mouvementRepository.save(mouvement);
    }

    private MouvementCaisse findWithinTenant(UUID id) {
        return mouvementRepository.findByIdAndEntrepriseId(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.MOUVEMENT_CAISSE_NOT_FOUND, "Mouvement de caisse introuvable"));
    }

    private MouvementCaisseResponse toResponse(MouvementCaisse m) {
        return new MouvementCaisseResponse(m.getId(), m.getDossierId(), m.getPaiementId(), m.getTypeMouvement(),
                m.getCategorie(), m.getLibelle(), m.getMontant(), m.getDevise(), m.getModePaiement(),
                m.getDateMouvement(), m.getReference(), m.getStatut());
    }
    /**
     * Correctif (session de test réelle) : le pilote JDBC PostgreSQL/Hibernate mal-type un
     * paramètre Instant null explicitement casté dans la requête (bytea au lieu de timestamp).
     * Plutôt que de dépendre du typage implicite d'un null, on substitue des bornes
     * "sentinelles" couvrant toutes les dates possibles — comportement identique pour
     * l'utilisateur (aucun filtre appliqué), mais aucune valeur null n'est jamais transmise.
     */
    private Instant borneDebutEffective(Instant dateDebut) {
        return dateDebut != null ? dateDebut : Instant.EPOCH;
    }

    private Instant borneFinEffective(Instant dateFin) {
        return dateFin != null ? dateFin : Instant.now().plus(java.time.Duration.ofDays(365 * 100));
    }
}
