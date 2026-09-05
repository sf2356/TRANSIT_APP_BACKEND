package com.transit.platform.marchandise.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MarchandiseResponse(
        UUID id, UUID dossierId, String dossierNumero, String designation, String description, String typeMarchandise, String statut,
        Integer nombreColis, String typeColis, BigDecimal poidsBrut, BigDecimal volumeTotal, String numeroConteneur,
        String typeConteneur, String documentTransport, String plomb, String origine, String destination, String natureMarchandise,
        String marqueReference, BigDecimal valeurDeclaree, String deviseValeur, String codeSH, String paysOrigine,
        String paysProvenance, String destinationFinale, String observations, String observationsDouane
) {}
