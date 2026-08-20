package com.transit.platform.facture;

import com.transit.platform.common.pdf.PdfDocumentBuilder;
import com.transit.platform.document.storage.FileStorageService;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierService;
import com.transit.platform.entreprise.Entreprise;
import com.transit.platform.entreprise.EntrepriseRepository;
import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Génère le PDF de facture/proforma (Prompt 01 §18). Le modèle visuel (MODERNE / CLASSIQUE
 * / MINIMALISTE) et la couleur d'accent viennent des paramètres de l'entreprise — les
 * DONNÉES transmises à PdfDocumentBuilder restent identiques quel que soit le modèle choisi
 * (demande utilisateur : "données standardisées, présentation personnalisable").
 */
@Service
public class FacturePdfService {

    private static final Logger log = LoggerFactory.getLogger(FacturePdfService.class);

    private final FactureService factureService;
    private final LigneFactureRepository ligneFactureRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ParametreEntrepriseRepository parametreRepository;
    private final TiersRepository tiersRepository;
    private final DossierService dossierService;
    private final TenantContext tenantContext;
    private final FileStorageService fileStorageService;

    public FacturePdfService(FactureService factureService, LigneFactureRepository ligneFactureRepository,
                             EntrepriseRepository entrepriseRepository, ParametreEntrepriseRepository parametreRepository,
                             TiersRepository tiersRepository, DossierService dossierService, TenantContext tenantContext,
                             FileStorageService fileStorageService) {
        this.factureService = factureService;
        this.ligneFactureRepository = ligneFactureRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.parametreRepository = parametreRepository;
        this.tiersRepository = tiersRepository;
        this.dossierService = dossierService;
        this.tenantContext = tenantContext;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public byte[] genererPdf(UUID factureId) {
        Facture facture = factureService.findWithinTenant(factureId);
        Entreprise entreprise = entrepriseRepository.findById(tenantContext.currentEntrepriseId()).orElseThrow();
        ParametreEntreprise parametres = parametreRepository.findByEntrepriseId(tenantContext.currentEntrepriseId()).orElse(null);
        Tiers client = tiersRepository.findById(facture.getClientId()).orElse(null);

        String referenceDossier = null;
        if (facture.getDossierId() != null) {
            try {
                Dossier dossier = dossierService.findWithinTenant(facture.getDossierId());
                referenceDossier = dossier.getNumero();
            } catch (Exception e) {
                log.warn("Dossier {} introuvable pour la r\u00e9f\u00e9rence PDF de la facture {}", facture.getDossierId(), facture.getNumero());
            }
        }

        List<PdfDocumentBuilder.LigneDocument> lignes = ligneFactureRepository.findByFactureIdOrderByOrdreAsc(factureId).stream()
                .map(l -> new PdfDocumentBuilder.LigneDocument(l.getDescription(), PdfDocumentBuilder.formatNombre(l.getQuantite()),
                        PdfDocumentBuilder.formatNombre(l.getPrixUnitaire()), PdfDocumentBuilder.formatNombre(l.getMontant())))
                .toList();

        var enTete = new PdfDocumentBuilder.EnTeteEntreprise(entreprise.getNom(), entreprise.getAdresse(),
                entreprise.getTelephone(), entreprise.getEmail(), entreprise.getSiteWeb(),
                entreprise.getRccm(), entreprise.getIfu(), telechargerImage(entreprise.getLogo()),
                entreprise.getSecteurActivite());

        List<String> clientLignes = client != null
                ? java.util.stream.Stream.of(client.getAdresse(),
                        java.util.stream.Stream.of(client.getVille(), client.getPays()).filter(s -> s != null && !s.isBlank())
                                .reduce((a, b) -> a + ", " + b).orElse(null),
                        client.getTelephone() != null ? "T\u00e9l : " + client.getTelephone() : null)
                .filter(s -> s != null && !s.isBlank()).toList()
                : List.of();

        var coordonneesBancaires = new PdfDocumentBuilder.CoordonneesBancaires(entreprise.getBanque(), entreprise.getIban());

        var signature = parametres != null
                ? new PdfDocumentBuilder.Signature(parametres.getNomSignataire(), parametres.getFonctionSignataire(),
                telechargerImage(entreprise.getCachet()))
                : null;

        return PdfDocumentBuilder.genererDocument(
                entreprise.getTemplatePdf(), entreprise.getCouleurAccent(),
                facture.getTypeDocument(), facture.getNumero(), facture.getStatut(), enTete,
                client != null ? client.getRaisonSociale() : null, clientLignes, referenceDossier,
                facture.getDateDocument().toString(), facture.getDateEcheance() != null ? facture.getDateEcheance().toString() : null,
                lignes, facture.getMontantHT(), facture.getMontantTaxe(), facture.getMontantTotal(), facture.getMontantPaye(),
                facture.getDevise(), facture.getNotes(), facture.getConditions(), coordonneesBancaires, signature,
                entreprise.getMentionsLegales());
    }

    private byte[] telechargerImage(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return null;
        try (var stream = fileStorageService.retrieve(storageKey)) {
            return stream.readAllBytes();
        } catch (Exception e) {
            log.warn("Impossible de charger l'image {} pour le PDF : {}", storageKey, e.getMessage());
            return null;
        }
    }
}