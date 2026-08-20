package com.transit.platform.cotation;

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
 * Génère le PDF de cotation/devis. Même modèle visuel et couleur d'accent que les
 * factures (paramètres entreprise) — cohérence demandée par l'utilisateur entre les deux
 * types de documents.
 */
@Service
public class CotationPdfService {

    private static final Logger log = LoggerFactory.getLogger(CotationPdfService.class);

    private final CotationRepository cotationRepository;
    private final LigneCotationRepository ligneCotationRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ParametreEntrepriseRepository parametreRepository;
    private final TiersRepository tiersRepository;
    private final DossierService dossierService;
    private final TenantContext tenantContext;
    private final FileStorageService fileStorageService;

    public CotationPdfService(CotationRepository cotationRepository, LigneCotationRepository ligneCotationRepository,
                              EntrepriseRepository entrepriseRepository, ParametreEntrepriseRepository parametreRepository,
                              TiersRepository tiersRepository, DossierService dossierService, TenantContext tenantContext,
                              FileStorageService fileStorageService) {
        this.cotationRepository = cotationRepository;
        this.ligneCotationRepository = ligneCotationRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.parametreRepository = parametreRepository;
        this.tiersRepository = tiersRepository;
        this.dossierService = dossierService;
        this.tenantContext = tenantContext;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public byte[] genererPdf(UUID cotationId) {
        Cotation cotation = cotationRepository.findByIdAndEntrepriseId(cotationId, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> com.transit.platform.common.BusinessException.notFound(
                        com.transit.platform.common.ErrorCode.COTATION_NOT_FOUND, "Cotation introuvable"));

        Entreprise entreprise = entrepriseRepository.findById(tenantContext.currentEntrepriseId()).orElseThrow();
        ParametreEntreprise parametres = parametreRepository.findByEntrepriseId(tenantContext.currentEntrepriseId()).orElse(null);
        Tiers client = tiersRepository.findById(cotation.getClientId()).orElse(null);

        String referenceDossier = null;
        if (cotation.getDossierId() != null) {
            try {
                Dossier dossier = dossierService.findWithinTenant(cotation.getDossierId());
                referenceDossier = dossier.getNumero();
            } catch (Exception e) {
                log.warn("Dossier {} introuvable pour la r\u00e9f\u00e9rence PDF de la cotation {}", cotation.getDossierId(), cotation.getNumero());
            }
        }

        List<PdfDocumentBuilder.LigneDocument> lignes = ligneCotationRepository.findByCotationIdOrderByOrdreAsc(cotationId).stream()
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
                "COTATION", cotation.getNumero(), cotation.getStatut(), enTete,
                client != null ? client.getRaisonSociale() : null, clientLignes, referenceDossier,
                cotation.getDateCotation().toString(), cotation.getDateValidite() != null ? cotation.getDateValidite().toString() : null,
                lignes, cotation.getMontantHT(), cotation.getMontantTaxe(), cotation.getMontantTotal(), null,
                cotation.getDevise(), cotation.getNotes(), cotation.getConditions(), coordonneesBancaires, signature,
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