package com.transit.platform.entreprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNumerotationRequest(
        @NotBlank @Size(max = 20) String prefixeDossier,
        @NotBlank @Size(max = 20) String prefixeCotation,
        @NotBlank @Size(max = 20) String prefixeFacture,
        @NotBlank @Size(max = 20) String prefixePaiement
) {}
