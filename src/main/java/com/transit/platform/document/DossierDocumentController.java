package com.transit.platform.document;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.PagedApiResponse;
import com.transit.platform.document.dto.DocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

/** Endpoint contextualisé (Prompt 01 §9) : upload direct depuis la fiche dossier, sans resélection. */
@RestController
@RequestMapping("/api/v1/dossiers/{dossierId}/documents")
@Tag(name = "Documents", description = "Documents d'un dossier — upload contextuel")
public class DossierDocumentController {

    private final DocumentService documentService;

    public DossierDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public PagedApiResponse<DocumentResponse> list(@PathVariable UUID dossierId, Pageable pageable) {
        return PagedApiResponse.of(documentService.listByDossier(dossierId, pageable));
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DOCUMENT_CREATE')")
    @Operation(summary = "Uploader un document dans le dossier (mobile : photo/scan depuis la caméra, cf. Prompt 01 §13)")
    public ApiResponse<DocumentResponse> upload(@PathVariable UUID dossierId,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam String titre,
                                                 @RequestParam String typeDocument,
                                                 @RequestParam(required = false) UUID marchandiseId,
                                                 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateReception,
                                                 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateExpiration) {
        return ApiResponse.of(documentService.upload(dossierId, file, titre, typeDocument, marchandiseId, dateReception, dateExpiration));
    }
}
