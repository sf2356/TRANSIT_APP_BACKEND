package com.transit.platform.document;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.document.dto.DocumentResponse;
import com.transit.platform.document.dto.UpdateDocumentMetadataRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Documents liés à un dossier, une marchandise, une facture ou une cotation")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public com.transit.platform.common.PagedApiResponse<DocumentResponse> search(
            @RequestParam(required = false) UUID dossierId,
            @RequestParam(required = false) String typeDocument,
            @RequestParam(required = false) String search,
            org.springframework.data.domain.Pageable pageable) {
        return com.transit.platform.common.PagedApiResponse.of(documentService.search(dossierId, typeDocument, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ApiResponse<DocumentResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(documentService.getById(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @Operation(summary = "Télécharger le document — redirige vers une URL signée (S3) ou streame le fichier (stockage local)")
    public ResponseEntity<?> download(@PathVariable UUID id) {
        DocumentService.DownloadHandle handle = documentService.download(id);
        if (handle.isRedirect()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(handle.redirectUrl())).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(handle.contentType() != null ? handle.contentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + handle.filename() + "\"")
                .body(new InputStreamResource(handle.stream()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE')")
    @Operation(summary = "Modifier les métadonnées d'un document (titre, type, dates) — pas le fichier lui-même")
    public ApiResponse<DocumentResponse> updateMetadata(@PathVariable UUID id, @Valid @RequestBody UpdateDocumentMetadataRequest request) {
        return ApiResponse.of(documentService.updateMetadata(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
    @Operation(summary = "Suppression logique — le fichier reste sur le stockage, traçabilité conservée")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ApiResponse.of(null);
    }
}
