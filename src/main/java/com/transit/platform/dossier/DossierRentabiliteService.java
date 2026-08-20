package com.transit.platform.dossier;

import com.transit.platform.charge.ChargeRepository;
import com.transit.platform.dossier.dto.DossierRentabiliteResponse;
import com.transit.platform.facture.FactureRepository;
import com.transit.platform.paiement.PaiementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Résout GET /dossiers/{id}/rentabilite, explicitement différé à l'étape 9 (Prompt 03 §19,
 * §32) faute des modules Facture/Paiement/Charge. Service séparé de DossierService pour
 * éviter que le module central "dossier" dépende directement des modules financiers
 * "aval" — c'est l'inverse qui est vrai dans le reste du code (facture/paiement/charge
 * dépendent de DossierService, jamais l'inverse). Ici l'exception est assumée : il s'agit
 * d'une lecture agrégée transverse, pas d'une dépendance métier structurelle.
 */
@Service
public class DossierRentabiliteService {

    private final DossierService dossierService;
    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final ChargeRepository chargeRepository;

    public DossierRentabiliteService(DossierService dossierService, FactureRepository factureRepository,
                                      PaiementRepository paiementRepository, ChargeRepository chargeRepository) {
        this.dossierService = dossierService;
        this.factureRepository = factureRepository;
        this.paiementRepository = paiementRepository;
        this.chargeRepository = chargeRepository;
    }

    @Transactional(readOnly = true)
    public DossierRentabiliteResponse calculer(UUID dossierId) {
        dossierService.findWithinTenant(dossierId); // valide l'appartenance au tenant avant toute agrégation

        BigDecimal totalFacture = factureRepository.sumMontantTotalByDossier(dossierId);
        BigDecimal totalEncaisse = paiementRepository.sumValideByDossier(dossierId);
        BigDecimal resteAEncaisser = totalFacture.subtract(totalEncaisse);

        BigDecimal totalCharges = chargeRepository.sumByDossier(dossierId);
        BigDecimal droitsTaxes = chargeRepository.sumByDossierAndType(dossierId, "DROITS_TAXES");
        BigDecimal transport = chargeRepository.sumByDossierAndType(dossierId, "TRANSPORT");
        BigDecimal manutention = chargeRepository.sumByDossierAndType(dossierId, "MANUTENTION");

        // Marge indicative = encaissé - charges (et non facturé - charges) : reflète la
        // rentabilité RÉELLE déjà matérialisée en trésorerie, cohérent avec l'exemple
        // fourni au Prompt 03 §32 où totalEncaisse > totalFacture est géré sans incohérence
        // (reste à encaisser peut alors être négatif, ce qui est correct et affiché tel quel).
        BigDecimal margeEstimee = totalEncaisse.subtract(totalCharges);

        return new DossierRentabiliteResponse(totalFacture, totalEncaisse, resteAEncaisser, totalCharges,
                droitsTaxes, transport, manutention, margeEstimee);
    }
}
