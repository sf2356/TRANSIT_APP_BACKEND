package com.transit.platform.marchandise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateMarchandiseRequest(
        @NotBlank(message = "La désignation est obligatoire") String designation,
        String description,
        String typeMarchandise,
        @PositiveOrZero Integer nombreColis,
        String typeColis,
        @PositiveOrZero BigDecimal poidsBrut,
        @PositiveOrZero BigDecimal volumeTotal,
        String numeroConteneur,
        String typeConteneur,
        String documentTransport,
        String plomb,
        String origine,
        String destination,
        String natureMarchandise,
        String marqueReference,
        @PositiveOrZero BigDecimal valeurDeclaree,
        String deviseValeur,
        String codeSH,
        String paysOrigine,
        String paysProvenance,
        String destinationFinale,
        String observations,
        String observationsDouane
) {}
