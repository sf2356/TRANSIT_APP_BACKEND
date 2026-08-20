package com.transit.platform.entreprise.dto;

import java.util.UUID;

public record EntrepriseResponse(
        UUID id, String nom, String email, String telephone, String adresse,
        String pays, String ville, String secteurActivite, String deviseDefaut,
        String logo, String typeActivite, String statut,
        String rccm, String ifu, String siteWeb, String banque, String iban, String cachet, String mentionsLegales,
        String templatePdf, String couleurAccent
) {}