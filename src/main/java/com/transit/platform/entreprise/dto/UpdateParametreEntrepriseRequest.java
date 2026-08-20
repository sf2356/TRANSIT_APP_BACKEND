package com.transit.platform.entreprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateParametreEntrepriseRequest(
        @NotBlank @Size(max = 20) String prefixeDossier,
        @NotBlank @Size(max = 20) String prefixeCotation,
        @NotBlank @Size(max = 20) String prefixeFacture,
        @NotBlank @Size(max = 20) String prefixePaiement,
        String signatureImage,
        String nomSignataire,
        String fonctionSignataire,
        @NotBlank String devise
) {}
