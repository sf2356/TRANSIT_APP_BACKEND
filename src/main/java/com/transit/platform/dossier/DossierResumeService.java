package com.transit.platform.dossier;

import com.transit.platform.cotation.CotationRepository;
import com.transit.platform.document.DocumentRepository;
import com.transit.platform.dossier.dto.DossierResumeResponse;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import com.transit.platform.facture.FactureRepository;
import com.transit.platform.marchandise.MarchandiseRepository;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import com.transit.platform.utilisateur.Utilisateur;
import com.transit.platform.utilisateur.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Compose DossierService + DossierRentabiliteService + compteurs pour livrer en un seul
 * appel tout ce dont la page de détail dossier a besoin (Prompt 04 §16/§38 — "réduire le
 * nombre d'appels API"). Comme DossierRentabiliteService, dépend "vers l'aval" des modules
 * financiers — exception assumée et documentée au même endroit (voir DossierRentabiliteService).
 */
@Service
public class DossierResumeService {

    private final DossierService dossierService;
    private final DossierRentabiliteService rentabiliteService;
    private final MarchandiseRepository marchandiseRepository;
    private final DocumentRepository documentRepository;
    private final CotationRepository cotationRepository;
    private final FactureRepository factureRepository;
    private final TiersRepository tiersRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreEntrepriseRepository parametreEntrepriseRepository;
    private final TenantContext tenantContext;

    public DossierResumeService(DossierService dossierService, DossierRentabiliteService rentabiliteService,
                                 MarchandiseRepository marchandiseRepository, DocumentRepository documentRepository,
                                 CotationRepository cotationRepository, FactureRepository factureRepository,
                                 TiersRepository tiersRepository, UtilisateurRepository utilisateurRepository,
                                 ParametreEntrepriseRepository parametreEntrepriseRepository, TenantContext tenantContext) {
        this.dossierService = dossierService;
        this.rentabiliteService = rentabiliteService;
        this.marchandiseRepository = marchandiseRepository;
        this.documentRepository = documentRepository;
        this.cotationRepository = cotationRepository;
        this.factureRepository = factureRepository;
        this.tiersRepository = tiersRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.parametreEntrepriseRepository = parametreEntrepriseRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public DossierResumeResponse resume(UUID dossierId) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Dossier dossier = dossierService.findWithinTenant(dossierId);

        var client = tiersRepository.findById(dossier.getClientId())
                .map(t -> new DossierResumeResponse.ClientResume(t.getId(), t.getRaisonSociale()))
                .orElse(null);
        var responsable = dossier.getResponsableId() != null
                ? utilisateurRepository.findById(dossier.getResponsableId())
                        .map(u -> new DossierResumeResponse.ResponsableResume(u.getId(), u.getNomComplet()))
                        .orElse(null)
                : null;

        var rentabilite = rentabiliteService.calculer(dossierId);
        String devise = parametreEntrepriseRepository.findByEntrepriseId(entrepriseId)
                .map(p -> p.getDevise()).orElse("XOF");

        return new DossierResumeResponse(
                dossier.getId(), dossier.getNumero(), dossier.getTitre(), dossier.getStatut(), dossier.getPriorite(),
                client, responsable, dossier.getDateOuverture(), dossier.getDateEcheance(),
                marchandiseRepository.countByDossierId(dossierId),
                documentRepository.countByDossierIdAndEntrepriseIdAndDeletedAtIsNull(dossierId, entrepriseId),
                cotationRepository.countByEntrepriseIdAndDossierId(entrepriseId, dossierId),
                factureRepository.countByEntrepriseIdAndDossierIdAndDeletedAtIsNull(entrepriseId, dossierId),
                rentabilite.totalFacture(), rentabilite.totalEncaisse(), rentabilite.resteAEncaisser(),
                rentabilite.totalCharges(), rentabilite.margeEstimee(), devise
        );
    }
}
