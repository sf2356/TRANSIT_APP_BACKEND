package com.transit.platform.recouvrement.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRelanceRequest(
        @NotBlank String statut,
        java.time.LocalDate prochaineRelance,
        String commentaire
) {}
