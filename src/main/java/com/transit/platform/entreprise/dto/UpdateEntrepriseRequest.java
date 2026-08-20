package com.transit.platform.entreprise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEntrepriseRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 255) String nom,
        @NotBlank @Email(message = "Email invalide") String email,
        String telephone,
        String adresse,
        String pays,
        String ville,
        String secteurActivite,
        @NotBlank String deviseDefaut,
        String typeActivite,
        String rccm,
        String ifu,
        String siteWeb,
        String banque,
        String iban,
        String mentionsLegales,
        @Pattern(regexp = "MODERNE|CLASSIQUE|MINIMALISTE", message = "Mod\u00e8le de facture invalide")
        String templatePdf,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "La couleur doit \u00eatre au format #RRGGBB")
        String couleurAccent
) {}