package com.transit.platform.referentiel;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique centralisé (Prompt 03 §58) : évite de dupliquer les valeurs
 * métier dans Angular et Flutter. Valeurs alignées sur les enums documentés au
 * Prompt 02 §38 (StatutDossier, Priorite, ModeTransport, TypeOperation, RegimeDouanier,
 * Incoterm, TypeMarchandise, ModePaiement, TypeCharge, TypeDocument, TypeRelance).
 *
 * Choix volontaire : catalogue codé en dur plutôt qu'en base — ces valeurs changent très
 * rarement et un accès direct évite une dépendance PostgreSQL pour peupler de simples
 * listes déroulantes. Migrable vers une table sans changement de contrat API si le besoin
 * de configuration par entreprise apparaît (cf. §38 du Prompt 02, non retenu en V1).
 */
public final class ReferentielCatalogue {

    private ReferentielCatalogue() {}

    public static final Map<String, List<ReferentielItem>> CATALOGUE = Map.ofEntries(
            Map.entry("modes-transport", List.of(
                    new ReferentielItem("MARITIME", "Maritime"),
                    new ReferentielItem("AERIEN", "Aérien"),
                    new ReferentielItem("ROUTIER", "Routier"),
                    new ReferentielItem("FERROVIAIRE", "Ferroviaire"),
                    new ReferentielItem("MULTIMODAL", "Multimodal"))),
            Map.entry("priorites", List.of(
                    new ReferentielItem("BASSE", "Basse"),
                    new ReferentielItem("NORMALE", "Normale"),
                    new ReferentielItem("HAUTE", "Haute"),
                    new ReferentielItem("URGENTE", "Urgente"))),
            Map.entry("types-operation", List.of(
                    new ReferentielItem("IMPORT", "Import"),
                    new ReferentielItem("EXPORT", "Export"),
                    new ReferentielItem("TRANSIT", "Transit"),
                    new ReferentielItem("TRANSBORDEMENT", "Transbordement"))),
            Map.entry("regimes-douaniers", List.of(
                    new ReferentielItem("MISE_A_LA_CONSOMMATION", "Mise à la consommation"),
                    new ReferentielItem("ENTREPOT", "Entrepôt"),
                    new ReferentielItem("TRANSIT", "Transit"),
                    new ReferentielItem("ADMISSION_TEMPORAIRE", "Admission temporaire"),
                    new ReferentielItem("EXPORTATION", "Exportation"),
                    new ReferentielItem("REEXPORTATION", "Réexportation"),
                    new ReferentielItem("AUTRE", "Autre"))),
            Map.entry("incoterms", List.of(
                    new ReferentielItem("EXW", "EXW — Ex Works"),
                    new ReferentielItem("FCA", "FCA — Free Carrier"),
                    new ReferentielItem("FOB", "FOB — Free On Board"),
                    new ReferentielItem("CFR", "CFR — Cost and Freight"),
                    new ReferentielItem("CIF", "CIF — Cost, Insurance and Freight"),
                    new ReferentielItem("CPT", "CPT — Carriage Paid To"),
                    new ReferentielItem("CIP", "CIP — Carriage and Insurance Paid To"),
                    new ReferentielItem("DAP", "DAP — Delivered At Place"),
                    new ReferentielItem("DPU", "DPU — Delivered at Place Unloaded"),
                    new ReferentielItem("DDP", "DDP — Delivered Duty Paid"))),
            Map.entry("types-marchandise", List.of(
                    new ReferentielItem("GENERALE", "Générale"),
                    new ReferentielItem("DANGEREUSE", "Dangereuse"),
                    new ReferentielItem("PERISSABLE", "Périssable"),
                    new ReferentielItem("VRAC", "Vrac"),
                    new ReferentielItem("CONTENEURISEE", "Conteneurisée"),
                    new ReferentielItem("AUTRE", "Autre"))),
            Map.entry("statuts-marchandise", List.of(
                    new ReferentielItem("DECLAREE", "Déclarée"),
                    new ReferentielItem("EN_TRANSIT", "En transit"),
                    new ReferentielItem("DEDOUANEE", "Dédouanée"),
                    new ReferentielItem("LIVREE", "Livrée"),
                    new ReferentielItem("BLOQUEE", "Bloquée"))),
            Map.entry("types-document", List.of(
                    new ReferentielItem("BOOKING", "Booking"),
                    new ReferentielItem("PHOTOS_CHARGEMENT", "Photos de chargement"),
                    new ReferentielItem("BL_DRAFT", "BL / Draft"),
                    new ReferentielItem("CONTRAT_PAIEMENT", "Contrat de paiement"),
                    new ReferentielItem("ECTN", "ECTN"),
                    new ReferentielItem("ALERTE_ARRIVEE", "Alerte d'arrivée"),
                    new ReferentielItem("RECU_PAIEMENT_TRANSPORT", "Reçu de paiement transport"),
                    new ReferentielItem("TELEX", "TELEX (Release)"),
                    new ReferentielItem("FACTURE_FOURNISSEUR", "Facture fournisseur"),
                    new ReferentielItem("CONNAISSEMENT", "Connaissement (B/L définitif)"),
                    new ReferentielItem("DECLARATION_DOUANE", "Déclaration en douane"),
                    new ReferentielItem("CERTIFICAT_ORIGINE", "Certificat d'origine"),
                    new ReferentielItem("PACKING_LIST", "Packing list"),
                    new ReferentielItem("ASSURANCE", "Attestation d'assurance"),
                    new ReferentielItem("AUTRE", "Autre"))),
            Map.entry("statuts-cotation", List.of(
                    new ReferentielItem("BROUILLON", "Brouillon"),
                    new ReferentielItem("ENVOYEE", "Envoyée"),
                    new ReferentielItem("ACCEPTEE", "Acceptée"),
                    new ReferentielItem("REFUSEE", "Refusée"),
                    new ReferentielItem("EXPIREE", "Expirée"))),
            Map.entry("statuts-facture", List.of(
                    new ReferentielItem("BROUILLON", "Brouillon"),
                    new ReferentielItem("EMISE", "Émise"),
                    new ReferentielItem("PARTIELLEMENT_PAYEE", "Partiellement payée"),
                    new ReferentielItem("PAYEE", "Payée"),
                    new ReferentielItem("EN_RETARD", "En retard"),
                    new ReferentielItem("ANNULEE", "Annulée"))),
            Map.entry("modes-paiement", List.of(
                    new ReferentielItem("ESPECES", "Espèces"),
                    new ReferentielItem("MOBILE_MONEY", "Mobile Money"),
                    new ReferentielItem("VIREMENT", "Virement"),
                    new ReferentielItem("CHEQUE", "Chèque"),
                    new ReferentielItem("CARTE_BANCAIRE", "Carte bancaire"),
                    new ReferentielItem("AUTRE", "Autre"))),
            Map.entry("types-charge", List.of(
                    new ReferentielItem("FRAIS_TRANSIT", "Frais de transit"),
                    new ReferentielItem("FRAIS_DOUANE", "Frais de douane"),
                    new ReferentielItem("TRANSPORT", "Transport"),
                    new ReferentielItem("MANUTENTION", "Manutention"),
                    new ReferentielItem("ACCONAGE", "Aconage"),
                    new ReferentielItem("DROITS_TAXES", "Droits et taxes"),
                    new ReferentielItem("FRAIS_PORTUAIRES", "Frais portuaires"),
                    new ReferentielItem("AUTRE", "Autre"))),
            Map.entry("types-relance", List.of(
                    new ReferentielItem("APPEL", "Appel téléphonique"),
                    new ReferentielItem("WHATSAPP", "WhatsApp"),
                    new ReferentielItem("EMAIL", "Email"),
                    new ReferentielItem("COURRIER", "Courrier"),
                    new ReferentielItem("VISITE", "Visite"),
                    new ReferentielItem("AUTRE", "Autre"))),
            Map.entry("types-tiers", List.of(
                    new ReferentielItem("CLIENT", "Client"),
                    new ReferentielItem("FOURNISSEUR", "Fournisseur"),
                    new ReferentielItem("PARTENAIRE", "Partenaire"),
                    new ReferentielItem("AUTRE", "Autre")))
    );
}
