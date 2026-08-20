package com.transit.platform.tiers.dto;

import java.util.UUID;

public record TiersResponse(
        UUID id, String raisonSociale, String nomContact, String type, String telephone, String email,
        String adresse, String ville, String pays, String identifiantFiscal, String registreCommerce,
        String boitePostale, String statut, String notes
) {}