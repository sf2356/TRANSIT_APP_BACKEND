package com.transit.platform.dossier.dto;

import java.time.LocalDate;

public record OrdreTransitRequest(String numero, LocalDate date, String referenceClient, String donneurOrdre) {}
