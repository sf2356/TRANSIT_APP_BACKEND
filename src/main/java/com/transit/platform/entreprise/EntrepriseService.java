package com.transit.platform.entreprise;

import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.document.storage.FileStorageService;
import com.transit.platform.entreprise.dto.*;
import com.transit.platform.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Entreprise : lit/modifie TOUJOURS l'entreprise du contexte tenant courant.
 * Aucune méthode n'accepte un entrepriseId venant du client — voir TenantContext.
 */
@Service
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final ParametreEntrepriseRepository parametreRepository;
    private final TenantContext tenantContext;
    private final FileStorageService fileStorageService;

    public EntrepriseService(EntrepriseRepository entrepriseRepository,
                              ParametreEntrepriseRepository parametreRepository,
                              TenantContext tenantContext,FileStorageService fileStorageService) {
        this.entrepriseRepository = entrepriseRepository;
        this.parametreRepository = parametreRepository;
        this.tenantContext = tenantContext;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public EntrepriseResponse getCurrent() {
        Entreprise entreprise = findCurrent();
        return toResponse(entreprise);
    }

    @Transactional
    public EntrepriseResponse update(UpdateEntrepriseRequest request) {
        Entreprise entreprise = findCurrent();
        entreprise.setNom(request.nom());
        entreprise.setEmail(request.email());
        entreprise.setTelephone(request.telephone());
        entreprise.setAdresse(request.adresse());
        entreprise.setPays(request.pays());
        entreprise.setVille(request.ville());
        entreprise.setSecteurActivite(request.secteurActivite());
        entreprise.setDeviseDefaut(request.deviseDefaut());
        entreprise.setTypeActivite(request.typeActivite());
        entreprise.setRccm(request.rccm());
        entreprise.setIfu(request.ifu());
        entreprise.setSiteWeb(request.siteWeb());
        entreprise.setBanque(request.banque());
        entreprise.setIban(request.iban());
        entreprise.setMentionsLegales(request.mentionsLegales());
        entreprise.setTemplatePdf(request.templatePdf() != null ? request.templatePdf() : "MODERNE");
        entreprise.setCouleurAccent(request.couleurAccent() != null ? request.couleurAccent() : "#1E3A5F");
        return toResponse(entrepriseRepository.save(entreprise));
    }

    /** Upload du logo — réutilise l'abstraction de stockage déjà en place pour les documents (Prompt 03 §17). */
    @Transactional
    public EntrepriseResponse uploadLogo(org.springframework.web.multipart.MultipartFile file) {
        Entreprise entreprise = findCurrent();
        var stored = uploadImage(file, "logos");
        entreprise.setLogo(stored.storageKey());
        return toResponse(entrepriseRepository.save(entreprise));
    }

    @Transactional(readOnly = true)
    public com.transit.platform.document.DocumentService.DownloadHandle telechargerImage(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw BusinessException.notFound(ErrorCode.VALIDATION_ERROR, "Aucune image enregistrée.");
        }
        java.util.Optional<String> presignedUrl = fileStorageService.generatePresignedDownloadUrl(storageKey, java.time.Duration.ofMinutes(15));
        if (presignedUrl.isPresent()) {
            return com.transit.platform.document.DocumentService.DownloadHandle.redirect(presignedUrl.get());
        }
        java.io.InputStream stream = fileStorageService.retrieve(storageKey);
        return com.transit.platform.document.DocumentService.DownloadHandle.stream(stream, "image", "application/octet-stream");
    }

    @Transactional(readOnly = true)
    public com.transit.platform.document.DocumentService.DownloadHandle telechargerLogo() {
        return telechargerImage(findCurrent().getLogo());
    }

    @Transactional(readOnly = true)
    public com.transit.platform.document.DocumentService.DownloadHandle telechargerCachet() {
        return telechargerImage(findCurrent().getCachet());
    }

    @Transactional
    public EntrepriseResponse uploadCachet(org.springframework.web.multipart.MultipartFile file) {
        Entreprise entreprise = findCurrent();
        var stored = uploadImage(file, "cachets");
        entreprise.setCachet(stored.storageKey());
        return toResponse(entrepriseRepository.save(entreprise));
    }

    private com.transit.platform.document.storage.StoredFile uploadImage(org.springframework.web.multipart.MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw com.transit.platform.common.BusinessException.unprocessable(
                    com.transit.platform.common.ErrorCode.VALIDATION_ERROR, "Aucun fichier fourni.");
        }
        try {
            String key = tenantContext.currentEntrepriseId() + "/" + prefix;
            return fileStorageService.store(key, file.getOriginalFilename(), file.getContentType(), file.getInputStream(), file.getSize());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Erreur lors de l'upload de l'image", e);
        }
    }

    @Transactional(readOnly = true)
    public ParametreEntrepriseResponse getParametres() {
        ParametreEntreprise p = findCurrentParametres();
        return toResponse(p);
    }

    @Transactional
    public ParametreEntrepriseResponse updateParametres(UpdateParametreEntrepriseRequest request) {
        ParametreEntreprise p = findCurrentParametres();
        p.setPrefixeDossier(request.prefixeDossier());
        p.setPrefixeCotation(request.prefixeCotation());
        p.setPrefixeFacture(request.prefixeFacture());
        p.setPrefixePaiement(request.prefixePaiement());
        p.setSignatureImage(request.signatureImage());
        p.setNomSignataire(request.nomSignataire());
        p.setFonctionSignataire(request.fonctionSignataire());
        p.setDevise(request.devise());
        return toResponse(parametreRepository.save(p));
    }

    /** Prompt 04 §31 : sous-ensemble "numérotation" du namespace /api/v1/parametres. */
    @Transactional(readOnly = true)
    public NumerotationResponse getNumerotation() {
        ParametreEntreprise p = findCurrentParametres();
        return new NumerotationResponse(p.getPrefixeDossier(), p.getPrefixeCotation(), p.getPrefixeFacture(), p.getPrefixePaiement());
    }

    @Transactional
    public NumerotationResponse updateNumerotation(UpdateNumerotationRequest request) {
        ParametreEntreprise p = findCurrentParametres();
        p.setPrefixeDossier(request.prefixeDossier());
        p.setPrefixeCotation(request.prefixeCotation());
        p.setPrefixeFacture(request.prefixeFacture());
        p.setPrefixePaiement(request.prefixePaiement());
        parametreRepository.save(p);
        return new NumerotationResponse(p.getPrefixeDossier(), p.getPrefixeCotation(), p.getPrefixeFacture(), p.getPrefixePaiement());
    }

    /** Prompt 04 §31 : sous-ensemble "signature" du namespace /api/v1/parametres. */
    @Transactional(readOnly = true)
    public SignatureResponse getSignature() {
        ParametreEntreprise p = findCurrentParametres();
        return new SignatureResponse(p.getSignatureImage(), p.getNomSignataire(), p.getFonctionSignataire());
    }

    @Transactional
    public SignatureResponse updateSignature(UpdateSignatureRequest request) {
        ParametreEntreprise p = findCurrentParametres();
        p.setSignatureImage(request.signatureImage());
        p.setNomSignataire(request.nomSignataire());
        p.setFonctionSignataire(request.fonctionSignataire());
        parametreRepository.save(p);
        return new SignatureResponse(p.getSignatureImage(), p.getNomSignataire(), p.getFonctionSignataire());
    }

    private Entreprise findCurrent() {
        return entrepriseRepository.findById(tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ENTREPRISE_NOT_FOUND, "Entreprise introuvable"));
    }

    private ParametreEntreprise findCurrentParametres() {
        return parametreRepository.findByEntrepriseId(tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ENTREPRISE_NOT_FOUND, "Paramètres introuvables"));
    }

    private EntrepriseResponse toResponse(Entreprise e) {
        return new EntrepriseResponse(e.getId(), e.getNom(), e.getEmail(), e.getTelephone(), e.getAdresse(),
                e.getPays(), e.getVille(), e.getSecteurActivite(), e.getDeviseDefaut(), e.getLogo(),
                e.getTypeActivite(), e.getStatut(), e.getRccm(), e.getIfu(), e.getSiteWeb(),
                e.getBanque(), e.getIban(), e.getCachet(), e.getMentionsLegales(),
                e.getTemplatePdf(), e.getCouleurAccent());
    }

    private ParametreEntrepriseResponse toResponse(ParametreEntreprise p) {
        return new ParametreEntrepriseResponse(p.getPrefixeDossier(), p.getPrefixeCotation(), p.getPrefixeFacture(),
                p.getPrefixePaiement(), p.getLogo(), p.getSignatureImage(), p.getNomSignataire(),
                p.getFonctionSignataire(), p.getDevise());
    }
}
