package com.transit.platform.tiers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTiersRequest(
        @NotBlank @Size(max = 255) String raisonSociale,
        String nomContact,
        String telephone,
        @Email String email,
        String adresse,
        String ville,
        String pays,
        String identifiantFiscal,
        String registreCommerce,
        String boitePostale,
        String notes
) {}