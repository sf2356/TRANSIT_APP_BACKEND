package com.transit.platform.document;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.document.dto.DocumentResponse;
import com.transit.platform.document.dto.UpdateDocumentMetadataRequest;
import com.transit.platform.document.storage.FileStorageService;
import com.transit.platform.document.storage.StoredFile;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierRepository;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.dossier.enums.EvenementHistorique;
import com.transit.platform.marchandise.MarchandiseRepository;
import com.transit.platform.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

    private final DocumentRepository documentRepository;
    private final MarchandiseRepository marchandiseRepository;
    private final DossierService dossierService;
    private final DossierRepository dossierRepository;
    private final FileStorageService fileStorageService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public DocumentService(DocumentRepository documentRepository, MarchandiseRepository marchandiseRepository,
                            DossierService dossierService, FileStorageService fileStorageService,
                            TenantContext tenantContext, AuditService auditService,DossierRepository dossierRepository) {
        this.documentRepository = documentRepository;
        this.marchandiseRepository = marchandiseRepository;
        this.dossierService = dossierService;
        this.fileStorageService = fileStorageService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.dossierRepository=dossierRepository;
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listByDossier(UUID dossierId, Pageable pageable) {
        dossierService.findWithinTenant(dossierId);
        return documentRepository.findByDossierIdAndEntrepriseIdAndDeletedAtIsNull(
                dossierId, tenantContext.currentEntrepriseId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id) {
        return toResponse(findWithinTenant(id));
    }

    /**
     * Upload contextualisé : POST /dossiers/{dossierId}/documents. Le fichier est envoyé au
     * FileStorageService configuré (local ou S3-compatible) AVANT toute écriture en base ;
     * si le stockage échoue, aucune ligne "documents" fantôme n'est créée.
     */
    @Transactional
    public DocumentResponse upload(UUID dossierId, MultipartFile file, String titre, String typeDocument,
                                    UUID marchandiseId, LocalDate dateReception, LocalDate dateExpiration) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Dossier dossier = dossierService.findWithinTenant(dossierId);

        if (marchandiseId != null) {
            marchandiseRepository.findByIdAndDossierId(marchandiseId, dossierId)
                    .orElseThrow(() -> BusinessException.notFound(ErrorCode.MARCHANDISE_NOT_FOUND,
                            "La marchandise indiquée n'appartient pas à ce dossier"));
        }

        StoredFile stored;
        try (InputStream in = file.getInputStream()) {
            String keyPrefix = entrepriseId + "/" + dossierId;
            stored = fileStorageService.store(keyPrefix, file.getOriginalFilename(), file.getContentType(), in, file.getSize());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Impossible de lire le fichier envoyé",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        Document document = new Document();
        document.setEntrepriseId(entrepriseId);
        document.setDossierId(dossierId);
        document.setMarchandiseId(marchandiseId);
        document.setTitre(titre);
        document.setTypeDocument(typeDocument);
        document.setCheminFichier(stored.storageKey());
        document.setNomFichier(file.getOriginalFilename());
        document.setTypeMime(file.getContentType());
        document.setTaille(stored.size());
        document.setDateReception(dateReception);
        document.setDateExpiration(dateExpiration);
        document.setAjoutePar(tenantContext.currentUtilisateurId());
        document = documentRepository.save(document);
        dossierService.demarrerSiOuvert(dossier);
        dossierService.recordHistorique(dossier, EvenementHistorique.DOCUMENT_ADDED, "Document ajouté : " + document.getTitre());
        auditService.log("CREATE", "DOCUMENT", document.getId(), null,
                Map.of("titre", document.getTitre(), "typeDocument", document.getTypeDocument()));

        return toResponse(document);
    }

    /**
     * Retourne soit une URL de téléchargement directe et temporaire (mode S3-compatible),
     * soit un flux à streamer par le contrôleur (mode local) — voir DocumentController.
     */
    @Transactional(readOnly = true)
    public DownloadHandle download(UUID id) {
        Document document = findWithinTenant(id);
        Optional<String> presignedUrl = fileStorageService.generatePresignedDownloadUrl(document.getCheminFichier(), PRESIGNED_URL_TTL);
        if (presignedUrl.isPresent()) {
            return DownloadHandle.redirect(presignedUrl.get());
        }
        InputStream stream = fileStorageService.retrieve(document.getCheminFichier());
        return DownloadHandle.stream(stream, document.getNomFichier(), document.getTypeMime());
    }

    @Transactional
    public DocumentResponse updateMetadata(UUID id, UpdateDocumentMetadataRequest request) {
        Document document = findWithinTenant(id);
        document.setTitre(request.titre());
        document.setTypeDocument(request.typeDocument());
        document.setDateReception(request.dateReception());
        document.setDateExpiration(request.dateExpiration());
        return toResponse(documentRepository.save(document));
    }

    /** Suppression logique uniquement : le fichier physique reste sur le stockage (conformité/traçabilité), voir Prompt 02 §11. */
    @Transactional
    public void delete(UUID id) {
        Document document = findWithinTenant(id);
        document.setDeletedAt(java.time.Instant.now());
        document.setStatut("SUPPRIME");
        documentRepository.save(document);
        auditService.log("DELETE", "DOCUMENT", document.getId(), Map.of("statut", "ACTIF"), Map.of("statut", "SUPPRIME"));
    }

    private Document findWithinTenant(UUID id) {
        return documentRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.DOCUMENT_NOT_FOUND, "Document introuvable"));
    }

    private DocumentResponse toResponse(Document d) {
        String dossierNumero = d.getDossierId() != null
                ? dossierRepository.findById(d.getDossierId()).map(Dossier::getNumero).orElse(null)
                : null;
        return toResponse(d, dossierNumero);
    }

    private DocumentResponse toResponse(Document d, String dossierNumero) {
        return new DocumentResponse(d.getId(), d.getDossierId(), dossierNumero, d.getMarchandiseId(), d.getFactureId(), d.getCotationId(),
                d.getTitre(), d.getTypeDocument(), d.getNomFichier(), d.getTypeMime(), d.getTaille(),
                d.getStatut(), d.getDateReception(), d.getDateExpiration(), d.getAjoutePar(), d.getDateAjout());
    }

    /** Petite abstraction pour laisser le contrôleur choisir entre redirection 302 (S3) et streaming (local). */
    public record DownloadHandle(boolean isRedirect, String redirectUrl, InputStream stream, String filename, String contentType) {
        public static DownloadHandle redirect(String url) {
            return new DownloadHandle(true, url, null, null, null);
        }
        public static DownloadHandle stream(InputStream stream, String filename, String contentType) {
            return new DownloadHandle(false, null, stream, filename, contentType);
        }

    }
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<DocumentResponse> search(UUID dossierId, String typeDocument, String search, Pageable pageable) {
        Page<Document> page = documentRepository.search(tenantContext.currentEntrepriseId(), dossierId, typeDocument, search, pageable);
        java.util.Set<UUID> dossierIds = page.getContent().stream()
                .map(Document::getDossierId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        java.util.Map<UUID, String> numeros = dossierRepository.findAllById(dossierIds).stream()
                .collect(java.util.stream.Collectors.toMap(Dossier::getId, Dossier::getNumero));
        return page.map(d -> toResponse(d, numeros.get(d.getDossierId())));
    }
}
