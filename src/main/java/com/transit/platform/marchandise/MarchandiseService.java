package com.transit.platform.marchandise;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.marchandise.dto.*;
import com.transit.platform.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Toute opération passe d'abord par DossierService.findWithinTenant(dossierId) : c'est ce
 * qui garantit qu'une marchandise ne peut jamais être créée, lue ou modifiée sur un dossier
 * n'appartenant pas à l'entreprise courante (marchandises n'a pas sa propre colonne
 * entreprise_id — cf. modèle validé Prompt 02 §17).
 */
@Service
public class MarchandiseService {

    private final MarchandiseRepository marchandiseRepository;
    private final DossierService dossierService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public MarchandiseService(MarchandiseRepository marchandiseRepository, DossierService dossierService,
                               TenantContext tenantContext, AuditService auditService) {
        this.marchandiseRepository = marchandiseRepository;
        this.dossierService = dossierService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<MarchandiseResponse> searchGlobal(String statut, String search, Pageable pageable) {
        String normalized = search == null ? null : "%" + search.toLowerCase() + "%";
        Page<Marchandise> page = marchandiseRepository.searchForEntreprise(tenantContext.currentEntrepriseId(), statut, normalized, pageable);

        Set<UUID> dossierIds = page.getContent().stream().map(Marchandise::getDossierId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> numerosParDossier = dossierService.findNumerosByIds(dossierIds);

        return page.map(m -> toResponse(m, numerosParDossier.get(m.getDossierId())));
    }

    @Transactional(readOnly = true)
    public Page<MarchandiseResponse> listByDossier(UUID dossierId, Pageable pageable) {
        dossierService.findWithinTenant(dossierId);
        return marchandiseRepository.findByDossierId(dossierId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MarchandiseResponse getById(UUID id) {
        return toResponse(findAccessible(id));
    }

    /** Endpoint contextualisé POST /dossiers/{dossierId}/marchandises (Prompt 01 §9). */
    @Transactional
    public MarchandiseResponse createForDossier(UUID dossierId, CreateMarchandiseRequest request) {
        Dossier dossier = dossierService.findWithinTenant(dossierId);

        Marchandise m = new Marchandise();
        m.setDossierId(dossier.getId());
        applyFields(m, request);
        m = marchandiseRepository.save(m);

        dossierService.demarrerSiOuvert(dossier);
        dossierService.recordHistorique(dossier, EvenementHistorique.MARCHANDISE_ADDED,
                "Marchandise ajoutée : " + m.getDesignation());
        auditService.log("CREATE", "MARCHANDISE", m.getId(), null, Map.of("designation", m.getDesignation()));

        return toResponse(m);
    }

    @Transactional
    public MarchandiseResponse update(UUID id, UpdateMarchandiseRequest request) {
        Marchandise m = findAccessible(id);
        applyFields(m, request);
        m.setUpdatedAt(Instant.now());
        return toResponse(marchandiseRepository.save(m));
    }

    @Transactional
    public MarchandiseResponse changeStatut(UUID id, ChangeStatutMarchandiseRequest request) {
        Marchandise m = findAccessible(id);
        m.setStatut(request.statut());
        m.setUpdatedAt(Instant.now());
        return toResponse(marchandiseRepository.save(m));
    }

    /** Résout la marchandise ET vérifie au passage que son dossier appartient au tenant courant. */
    private Marchandise findAccessible(UUID id) {
        Marchandise m = marchandiseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.MARCHANDISE_NOT_FOUND, "Marchandise introuvable"));
        dossierService.findWithinTenant(m.getDossierId()); // lève DOSSIER_NOT_FOUND si tenant différent
        return m;
    }

    private void applyFields(Marchandise m, CreateMarchandiseRequest r) {
        m.setDesignation(r.designation());
        m.setDescription(r.description());
        m.setTypeMarchandise(r.typeMarchandise());
        m.setNombreColis(r.nombreColis());
        m.setTypeColis(r.typeColis());
        m.setPoidsBrut(r.poidsBrut());
        m.setVolumeTotal(r.volumeTotal());
        m.setNumeroConteneur(r.numeroConteneur());
        m.setTypeConteneur(r.typeConteneur());
        m.setDocumentTransport(r.documentTransport());
        m.setPlomb(r.plomb());
        m.setOrigine(r.origine());
        m.setDestination(r.destination());
        m.setNatureMarchandise(r.natureMarchandise());
        m.setMarqueReference(r.marqueReference());
        m.setValeurDeclaree(r.valeurDeclaree());
        m.setDeviseValeur(r.deviseValeur());
        m.setCodeSH(r.codeSH());
        m.setPaysOrigine(r.paysOrigine());
        m.setPaysProvenance(r.paysProvenance());
        m.setDestinationFinale(r.destinationFinale());
        m.setObservations(r.observations());
        m.setObservationsDouane(r.observationsDouane());
    }

    private void applyFields(Marchandise m, UpdateMarchandiseRequest r) {
        m.setDesignation(r.designation());
        m.setDescription(r.description());
        m.setTypeMarchandise(r.typeMarchandise());
        m.setNombreColis(r.nombreColis());
        m.setTypeColis(r.typeColis());
        m.setPoidsBrut(r.poidsBrut());
        m.setVolumeTotal(r.volumeTotal());
        m.setNumeroConteneur(r.numeroConteneur());
        m.setTypeConteneur(r.typeConteneur());
        m.setDocumentTransport(r.documentTransport());
        m.setPlomb(r.plomb());
        m.setOrigine(r.origine());
        m.setDestination(r.destination());
        m.setNatureMarchandise(r.natureMarchandise());
        m.setMarqueReference(r.marqueReference());
        m.setValeurDeclaree(r.valeurDeclaree());
        m.setDeviseValeur(r.deviseValeur());
        m.setCodeSH(r.codeSH());
        m.setPaysOrigine(r.paysOrigine());
        m.setPaysProvenance(r.paysProvenance());
        m.setDestinationFinale(r.destinationFinale());
        m.setObservations(r.observations());
        m.setObservationsDouane(r.observationsDouane());
    }

    private MarchandiseResponse toResponse(Marchandise m) {
        return toResponse(m, null);
    }

    private MarchandiseResponse toResponse(Marchandise m, String dossierNumero) {
        return new MarchandiseResponse(m.getId(), m.getDossierId(), dossierNumero, m.getDesignation(), m.getDescription(),
                m.getTypeMarchandise(), m.getStatut(), m.getNombreColis(), m.getTypeColis(), m.getPoidsBrut(),
                m.getVolumeTotal(), m.getNumeroConteneur(), m.getTypeConteneur(), m.getDocumentTransport(), m.getPlomb(), m.getOrigine(),
                m.getDestination(), m.getNatureMarchandise(), m.getMarqueReference(), m.getValeurDeclaree(),
                m.getDeviseValeur(), m.getCodeSH(), m.getPaysOrigine(), m.getPaysProvenance(),
                m.getDestinationFinale(), m.getObservations(), m.getObservationsDouane());
    }
}
