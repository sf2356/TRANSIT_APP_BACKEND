package com.transit.platform.document.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateDocumentMetadataRequest(
        @NotBlank String titre,
        @NotBlank String typeDocument,
        LocalDate dateReception,
        LocalDate dateExpiration
) {}
