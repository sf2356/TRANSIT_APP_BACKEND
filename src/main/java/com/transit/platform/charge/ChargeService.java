package com.transit.platform.charge;

import com.transit.platform.audit.AuditService;
import com.transit.platform.charge.dto.*;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.TiersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChargeService {

    private final ChargeRepository chargeRepository;
    private final TiersRepository tiersRepository;
    private final DossierService dossierService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public ChargeService(ChargeRepository chargeRepository, TiersRepository tiersRepository,
                          DossierService dossierService, TenantContext tenantContext, AuditService auditService) {
        this.chargeRepository = chargeRepository;
        this.tiersRepository = tiersRepository;
        this.dossierService = dossierService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ChargeResponse> search(UUID dossierId, String categorie, UUID fournisseurId, String statut,
                                       LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        LocalDate dateDebutEffective = dateDebut != null ? dateDebut : LocalDate.of(1900, 1, 1);
        LocalDate dateFinEffective = dateFin != null ? dateFin : LocalDate.of(2100, 12, 31);
        Page<Charge> page = chargeRepository.search(tenantContext.currentEntrepriseId(), dossierId, categorie, fournisseurId,
                statut, dateDebutEffective, dateFinEffective, pageable);

        Set<UUID> dossierIds = page.getContent().stream().map(Charge::getDossierId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> numerosParDossier = dossierService.findNumerosByIds(dossierIds);

        return page.map(c -> toResponse(c, numerosParDossier.get(c.getDossierId())));
    }

    @Transactional(readOnly = true)
    public ChargeResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    /** Endpoint contextualisé POST /dossiers/{dossierId}/charges (Prompt 01 §9). */
    @Transactional
    public ChargeResponse createForDossier(UUID dossierId, CreateChargeRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Dossier dossier = dossierService.findWithinTenant(dossierId);

        if (request.fournisseurId() != null) {
            tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(request.fournisseurId(), entrepriseId)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.TIERS_NOT_FOUND, "Fournisseur introuvable pour cette entreprise"));
        }

        Charge charge = new Charge();
        charge.setEntrepriseId(entrepriseId);
        charge.setDossierId(dossier.getId());
        charge.setFournisseurId(request.fournisseurId());
        charge.setLibelle(request.libelle());
        charge.setType(request.type());
        charge.setCategorie(request.categorie());
        charge.setMontant(request.montant());
        charge.setDevise(request.devise() != null ? request.devise() : "XOF");
        charge.setDateCharge(request.dateCharge() != null ? request.dateCharge() : LocalDate.now());
        charge.setReference(request.reference());
        charge.setNotes(request.notes());
        charge.setStatut("EN_ATTENTE");
        charge.setCreatedBy(tenantContext.currentUtilisateurId());
        charge = chargeRepository.save(charge);

        dossierService.recordHistorique(dossier, EvenementHistorique.CHARGE_CREATED, "Charge ajoutée : " + charge.getLibelle());
        auditService.log("CREATE", "CHARGE", charge.getId(), null, Map.of("libelle", charge.getLibelle(), "montant", charge.getMontant()));

        return toResponse(charge);
    }

    @Transactional
    public ChargeResponse update(UUID id, UpdateChargeRequest request) {
        Charge charge = findWithinTenant(id);
        if (request.fournisseurId() != null) {
            tiersRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(request.fournisseurId(), tenantContext.currentEntrepriseId())
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.TIERS_NOT_FOUND, "Fournisseur introuvable pour cette entreprise"));
        }
        charge.setFournisseurId(request.fournisseurId());
        charge.setLibelle(request.libelle());
        charge.setType(request.type());
        charge.setCategorie(request.categorie());
        charge.setMontant(request.montant());
        if (request.devise() != null) charge.setDevise(request.devise());
        if (request.dateCharge() != null) charge.setDateCharge(request.dateCharge());
        charge.setReference(request.reference());
        charge.setNotes(request.notes());
        charge.setUpdatedAt(java.time.Instant.now());
        return toResponse(chargeRepository.save(charge));
    }

    @Transactional
    public void annuler(UUID id) {
        Charge charge = findWithinTenant(id);
        charge.setStatut("REJETEE");
        charge.setUpdatedAt(java.time.Instant.now());
        chargeRepository.save(charge);
        auditService.log("CANCEL", "CHARGE", charge.getId(), null, Map.of("statut", "REJETEE"));
    }

    /**
     * Confirme le paiement d'un débours/charge (retour utilisateur terrain) — permet de
     * distinguer "la charge est enregistrée" (EN_ATTENTE, dès sa création) de "elle a
     * réellement été réglée" (PAYEE), utile pour le suivi de trésorerie par dossier.
     */
    @Transactional
    public void marquerPayee(UUID id) {
        Charge charge = findWithinTenant(id);
        charge.setStatut("PAYEE");
        charge.setUpdatedAt(java.time.Instant.now());
        chargeRepository.save(charge);
        auditService.log("PAY", "CHARGE", charge.getId(), null, Map.of("statut", "PAYEE"));
    }

    /** Utilisé par DashboardService / rentabilité (étape 20) pour sommer les charges d'un dossier sans dupliquer le contrôle tenant. */
    @Transactional(readOnly = true)
    public java.util.List<Charge> findAllForDossier(UUID dossierId) {
        dossierService.findWithinTenant(dossierId);
        return chargeRepository.findByDossierId(dossierId);
    }

    private Charge findWithinTenant(UUID id) {
        return chargeRepository.findByIdAndEntrepriseId(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.CHARGE_NOT_FOUND, "Charge introuvable"));
    }

    private ChargeResponse toResponse(Charge c) {
        return toResponse(c, null);
    }

    private ChargeResponse toResponse(Charge c, String dossierNumero) {
        return new ChargeResponse(c.getId(), c.getDossierId(), dossierNumero, c.getFournisseurId(), c.getLibelle(), c.getType(),
                c.getCategorie(), c.getMontant(), c.getDevise(), c.getStatut(), c.getDateCharge(), c.getReference(), c.getNotes());
    }
}
