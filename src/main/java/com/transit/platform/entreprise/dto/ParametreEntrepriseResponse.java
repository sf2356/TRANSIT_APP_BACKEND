package com.transit.platform.entreprise.dto;

public record ParametreEntrepriseResponse(
        String prefixeDossier, String prefixeCotation, String prefixeFacture, String prefixePaiement,
        String logo, String signatureImage, String nomSignataire, String fonctionSignataire, String devise
) {}
