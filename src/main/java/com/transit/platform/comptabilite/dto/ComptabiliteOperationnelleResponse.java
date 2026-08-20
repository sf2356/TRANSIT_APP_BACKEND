package com.transit.platform.comptabilite.dto;

import java.math.BigDecimal;

/**
 * Pré-comptabilité opérationnelle (Prompt 03 §33) — ne prétend PAS remplacer un logiciel
 * comptable complet : c'est une consolidation de lecture, pas un grand livre.
 */
public record ComptabiliteOperationnelleResponse(
        BigDecimal totalFacture, BigDecimal totalEncaisse, BigDecimal reste, BigDecimal charges,
        BigDecimal entreesCaisse, BigDecimal sortiesCaisse, BigDecimal soldeCaisse, BigDecimal resultatOperationnel
) {}
