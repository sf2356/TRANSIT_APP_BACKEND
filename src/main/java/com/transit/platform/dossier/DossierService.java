package com.transit.platform.dossier;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.dossier.dto.*;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.dossier.enums.StatutDossier;
import com.transit.platform.reference.ReferenceGeneratorService;
import com.transit.platform.reference.ReferenceType;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Dossier = entité pivot de la plateforme (Prompt 01 §7). Ce service illustre le patron à
 * reproduire pour les modules suivants (cotation, facture, paiement...) : validation du
 * tenant, génération automatique de numéro, écriture de l'historique métier + de l'audit,
 * dans une seule transaction cohérente.
 */
@Service
public class DossierService {

    private final DossierRepository dossierRepository;
    private final DossierHistoriqueRepository historiqueRepository;
    private final TiersRepository tiersRepository;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public DossierService(DossierRepository dossierRepository, DossierHistoriqueRepository historiqueRepository,
                           TiersRepository tiersRepository, ReferenceGeneratorService referenceGeneratorService,
                           TenantContext tenantContext, AuditService auditService) {
        this.dossierRepository = dossierRepository;
        this.historiqueRepository = historiqueRepository;
        this.tiersRepository = tiersRepository;
        this.referenceGeneratorService = referenceGeneratorService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<DossierSummaryResponse> search(String statut, UUID clientId, UUID responsableId, String search, Pageable pageable) {
        String normalized = search == null ? null : "%" + search.toLowerCase() + "%";
        return dossierRepository.search(tenantContext.currentEntrepriseId(), statut, clientId, responsableId, normalized, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public DossierResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    @Transactional
    public DossierResponse create(CreateDossierRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();

        // Le client doit exister ET appartenir à l'entreprise courante — jamais de confiance
        // dans un identifiant transmis par le client HTTP sans revérification serveur.
        Tiers client = tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(request.clientId(), entrepriseId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.CLIENT_NOT_FOUND, "Client introuvable pour cette entreprise"));

        Dossier dossier = new Dossier();
        dossier.setEntrepriseId(entrepriseId);
        dossier.setNumero(referenceGeneratorService.generate(entrepriseId, ReferenceType.DOSSIER)); // jamais saisi par l'utilisateur
        dossier.setClientId(client.getId());
        dossier.setTitre(request.titre());
        dossier.setModeTransport(request.modeTransport());
        dossier.setPriorite(request.priorite() != null ? request.priorite() : "NORMALE");
        dossier.setResponsableId(request.responsableId());
        dossier.setDateEcheance(request.dateEcheance());
        if (request.ordreTransit() != null) {
            dossier.setNumeroOrdreTransit(request.ordreTransit().numero());
            dossier.setDateOrdreTransit(request.ordreTransit().date());
            dossier.setReferenceClient(request.ordreTransit().referenceClient());
            dossier.setDonneurOrdre(request.ordreTransit().donneurOrdre());
        }
        if (request.douane() != null) {
            dossier.setTypeOperation(request.douane().typeOperation());
            dossier.setRegimeDouanier(request.douane().regimeDouanier());
            dossier.setIncoterm(request.douane().incoterm());
        }
        if (request.trajet() != null) {
            dossier.setOrigine(request.trajet().origine());
            dossier.setProvenance(request.trajet().provenance());
            dossier.setDestination(request.trajet().destination());
        }
        dossier.setInstructions(request.instructions());
        dossier.setDescription(request.description());
        dossier.setNotes(request.notes());
        dossier.setStatut(StatutDossier.OUVERT.name());
        dossier.setCreatedBy(tenantContext.currentUtilisateurId());
        dossier.setUpdatedBy(tenantContext.currentUtilisateurId());

        dossier = dossierRepository.save(dossier);

        recordHistorique(dossier, EvenementHistorique.DOSSIER_CREATED, "Dossier " + dossier.getNumero() + " créé");
        auditService.log("CREATE", "DOSSIER", dossier.getId(), null, Map.of("numero", dossier.getNumero(), "titre", dossier.getTitre()));

        return toResponse(dossier);
    }

    @Transactional
    public DossierResponse update(UUID id, UpdateDossierRequest request) {
        Dossier dossier = findWithinTenant(id);
        UUID entrepriseId = tenantContext.currentEntrepriseId();

        if (!dossier.getClientId().equals(request.clientId())) {
            tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(request.clientId(), entrepriseId)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.CLIENT_NOT_FOUND, "Client introuvable pour cette entreprise"));
            dossier.setClientId(request.clientId());
        }

        dossier.setTitre(request.titre());
        dossier.setModeTransport(request.modeTransport());
        dossier.setPriorite(request.priorite());
        dossier.setDateEcheance(request.dateEcheance());
        dossier.setNumeroOrdreTransit(request.numeroOrdreTransit());
        dossier.setDateOrdreTransit(request.dateOrdreTransit());
        dossier.setReferenceClient(request.referenceClient());
        dossier.setDonneurOrdre(request.donneurOrdre());
        dossier.setTypeOperation(request.typeOperation());
        dossier.setRegimeDouanier(request.regimeDouanier());
        dossier.setIncoterm(request.incoterm());
        dossier.setOrigine(request.origine());
        dossier.setProvenance(request.provenance());
        dossier.setDestination(request.destination());
        dossier.setInstructions(request.instructions());
        dossier.setDescription(request.description());
        dossier.setNotes(request.notes());
        dossier.setUpdatedBy(tenantContext.currentUtilisateurId());

        dossier = dossierRepository.save(dossier);
        recordHistorique(dossier, EvenementHistorique.DOSSIER_UPDATED, "Dossier modifié");
        return toResponse(dossier);
    }

    private static final java.util.Set<String> STATUTS_MANUELS = java.util.Set.of("EN_ATTENTE", "BLOQUE", "TERMINE", "ANNULE");
    @Transactional
    public DossierResponse changeStatut(UUID id, ChangeStatutRequest request) {
        Dossier dossier = findWithinTenant(id);
        StatutDossier nouveauStatut = parseStatut(request.statut());

        if (!STATUTS_MANUELS.contains(nouveauStatut.name())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Statut '" + nouveauStatut + "' non assignable manuellement (OUVERT/EN_COURS sont automatiques, CLOTURE passe par /cloturer)");
        }
        if (dossier.getStatut().equals(StatutDossier.CLOTURE.name()) || dossier.getStatut().equals(StatutDossier.ANNULE.name())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Impossible de changer le statut d'un dossier clôturé ou annulé");
        }

        String ancienStatut = dossier.getStatut();
        dossier.setStatut(nouveauStatut.name());
        dossier.setUpdatedBy(tenantContext.currentUtilisateurId());
        dossier = dossierRepository.save(dossier);

        recordHistorique(dossier, EvenementHistorique.STATUS_CHANGED, "Statut : " + ancienStatut + " → " + nouveauStatut);
        auditService.log("UPDATE_STATUS", "DOSSIER", dossier.getId(),
                Map.of("statut", ancienStatut), Map.of("statut", nouveauStatut.name()));
        return toResponse(dossier);
    }

    @Transactional
    public DossierResponse changeResponsable(UUID id, ChangeResponsableRequest request) {
        Dossier dossier = findWithinTenant(id);
        dossier.setResponsableId(request.responsableId());
        dossier.setUpdatedBy(tenantContext.currentUtilisateurId());
        dossier = dossierRepository.save(dossier);
        recordHistorique(dossier, EvenementHistorique.RESPONSABLE_CHANGED, "Responsable modifié");
        return toResponse(dossier);
    }

    @Transactional
    public DossierResponse cloturer(UUID id) {
        Dossier dossier = findWithinTenant(id);
        if (!dossier.getStatut().equals(StatutDossier.TERMINE.name())) {
            throw BusinessException.unprocessable(ErrorCode.INVALID_STATUS,
                    "Seul un dossier au statut TERMINE peut être clôturé");
        }
        dossier.setStatut(StatutDossier.CLOTURE.name());
        dossier.setDateCloture(java.time.LocalDate.now());
        dossier.setUpdatedBy(tenantContext.currentUtilisateurId());
        dossier = dossierRepository.save(dossier);

        recordHistorique(dossier, EvenementHistorique.DOSSIER_CLOSED, "Dossier clôturé");
        auditService.log("CLOSE", "DOSSIER", dossier.getId(), null, Map.of("statut", "CLOTURE"));
        return toResponse(dossier);
    }

    @Transactional(readOnly = true)
    public Page<DossierHistoriqueResponse> getHistorique(UUID id, Pageable pageable) {
        findWithinTenant(id); // valide l'accès tenant avant d'exposer l'historique
        return historiqueRepository.findByEntrepriseIdAndDossierIdOrderByDateEvenementDesc(
                        tenantContext.currentEntrepriseId(), id, pageable)
                .map(h -> new DossierHistoriqueResponse(h.getId(), h.getEvenement(), h.getDescription(),
                        h.getUtilisateurId(), h.getDateEvenement()));
    }

    /**
     * Point d'extension utilisé par les futurs modules (marchandise, document, cotation,
     * facture, paiement, charge, validation) pour alimenter la timeline du dossier sans
     * dupliquer cette logique — cf. Prompt 03 §41.
     */
    @Transactional
    public void recordHistorique(Dossier dossier, EvenementHistorique evenement, String description) {
        DossierHistorique h = new DossierHistorique();
        h.setEntrepriseId(dossier.getEntrepriseId());
        h.setDossierId(dossier.getId());
        h.setUtilisateurId(tenantContext.currentUtilisateurId());
        h.setEvenement(evenement.name());
        h.setDescription(description);
        historiqueRepository.save(h);
    }

    /**
     * Transition automatique OUVERT → EN_COURS, déclenchée par la première action opérationnelle
     * concrète sur le dossier (marchandise, document, cotation ou facture). Idempotente : ne
     * fait rien si le dossier n'est pas actuellement OUVERT.
     */
    @Transactional
    public void demarrerSiOuvert(Dossier dossier) {
        if ("OUVERT".equals(dossier.getStatut())) {
            dossier.setStatut("EN_COURS");
            dossierRepository.save(dossier);
            recordHistorique(dossier, EvenementHistorique.STATUS_CHANGED, "Statut : OUVERT → EN_COURS (première action)");
        }
    }

    /** Utilisé par les modules dépendants (facture, marchandise...) pour valider le dossier parent sans dupliquer le contrôle tenant. */
    @Transactional(readOnly = true)
    public Dossier findWithinTenant(UUID id) {
        return dossierRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.DOSSIER_NOT_FOUND, "Dossier introuvable"));
    }

    /**
     * Résout en UN SEUL appel les numéros de dossier pour un lot d'identifiants — évite le
     * problème classique des N+1 requêtes (une requête par ligne) quand une liste affiche le
     * numéro de dossier associé (Cotations, Factures, Marchandises...). Réutilisée par les
     * 3 services concernés plutôt que dupliquée. Ignore silencieusement les identifiants
     * inexistants ou hors du tenant courant — cet enrichissement est un confort d'affichage,
     * jamais une vérification de sécurité (qui reste faite ailleurs, via findWithinTenant).
     */
    @Transactional(readOnly = true)
    public java.util.Map<UUID, String> findNumerosByIds(java.util.Set<UUID> dossierIds) {
        if (dossierIds.isEmpty()) return java.util.Map.of();
        return dossierRepository.findAllById(dossierIds).stream()
                .filter(d -> d.getEntrepriseId().equals(tenantContext.currentEntrepriseId()))
                .collect(java.util.stream.Collectors.toMap(Dossier::getId, Dossier::getNumero));
    }

    private StatutDossier parseStatut(String statut) {
        try {
            return StatutDossier.valueOf(statut);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest(ErrorCode.INVALID_STATUS, "Statut de dossier invalide : " + statut);
        }
    }

    private DossierResponse toResponse(Dossier d) {
        return new DossierResponse(d.getId(), d.getNumero(), d.getClientId(), d.getTitre(), d.getModeTransport(),
                d.getPriorite(), d.getResponsableId(), d.getDateOuverture(), d.getDateEcheance(), d.getDateCloture(),
                d.getStatut(), d.getNumeroOrdreTransit(), d.getDateOrdreTransit(), d.getReferenceClient(),
                d.getDonneurOrdre(), d.getTypeOperation(), d.getRegimeDouanier(), d.getIncoterm(), d.getOrigine(),
                d.getProvenance(), d.getDestination(), d.getInstructions(), d.getDescription(), d.getNotes());
    }

    private DossierSummaryResponse toSummary(Dossier d) {
        return new DossierSummaryResponse(d.getId(), d.getNumero(), d.getTitre(), d.getClientId(), d.getStatut(),
                d.getPriorite(), d.getResponsableId(), d.getDateOuverture(), d.getDateEcheance());
    }
}
