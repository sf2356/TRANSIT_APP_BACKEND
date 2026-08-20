package com.transit.platform.dashboard;

import com.transit.platform.caisse.MouvementCaisseRepository;
import com.transit.platform.charge.ChargeRepository;
import com.transit.platform.dashboard.dto.*;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierRepository;
import com.transit.platform.facture.FactureRepository;
import com.transit.platform.paiement.PaiementRepository;
import com.transit.platform.recouvrement.RelanceRepository;
import com.transit.platform.security.TenantContext;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Toutes les données sont calculées côté backend (Prompt 03 §31) — jamais renvoyées comme
 * de simples listes brutes que le frontend devrait agréger lui-même.
 *
 * NOTE PERFORMANCE (à traiter à l'étape 23 "Optimisation") : le classement des dossiers
 * rentables/à risque interroge le détail de chaque dossier candidat (N+1 assumé sur un
 * échantillon borné de 50 dossiers récents) plutôt qu'une requête SQL agrégée unique.
 * Acceptable pour la V1 vu le volume attendu ; à remplacer par une vue matérialisée ou une
 * requête agrégée si la volumétrie grandit significativement.
 */
@Service
public class DashboardService {

    private static final int ECHANTILLON_RENTABILITE = 50;
    private static final int TOP_N = 5;

    private final DossierRepository dossierRepository;
    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final ChargeRepository chargeRepository;
    private final TiersRepository tiersRepository;
    private final RelanceRepository relanceRepository;
    private final MouvementCaisseRepository mouvementCaisseRepository;
    private final TenantContext tenantContext;

    public DashboardService(DossierRepository dossierRepository, FactureRepository factureRepository,
                             PaiementRepository paiementRepository, ChargeRepository chargeRepository,
                             TiersRepository tiersRepository, RelanceRepository relanceRepository,
                             MouvementCaisseRepository mouvementCaisseRepository, TenantContext tenantContext) {
        this.dossierRepository = dossierRepository;
        this.factureRepository = factureRepository;
        this.paiementRepository = paiementRepository;
        this.chargeRepository = chargeRepository;
        this.tiersRepository = tiersRepository;
        this.relanceRepository = relanceRepository;
        this.mouvementCaisseRepository = mouvementCaisseRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public DashboardGlobalResponse global() {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Map<String, Long> parStatut = dossiersParStatut(entrepriseId);
        BigDecimal totalFacture = factureRepository.sumMontantTotalByEntreprise(entrepriseId);
        BigDecimal totalEncaisse = paiementRepository.sumValideByEntreprise(entrepriseId);
        BigDecimal totalCharges = chargeRepository.sumByEntreprise(entrepriseId);
        LocalDate today = LocalDate.now();

        return new DashboardGlobalResponse(
                parStatut.values().stream().mapToLong(Long::longValue).sum(),
                parStatut,
                dossierRepository.countProchesEcheance(entrepriseId, today, today.plusDays(7)),
                totalFacture, totalEncaisse, factureRepository.sumResteAPayerByEntreprise(entrepriseId), totalCharges,
                totalEncaisse.subtract(totalCharges),
                tauxEncaissement(totalEncaisse, totalFacture),
                factureRepository.countFacturesEnRetard(entrepriseId, today),
                factureRepository.sumMontantEnRetard(entrepriseId, today));
    }

    @Transactional(readOnly = true)
    public DashboardDirectionResponse direction() {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        BigDecimal totalFacture = factureRepository.sumMontantTotalByEntreprise(entrepriseId);
        BigDecimal totalEncaisse = paiementRepository.sumValideByEntreprise(entrepriseId);
        BigDecimal totalCharges = chargeRepository.sumByEntreprise(entrepriseId);

        List<ClientMontantResponse> topClients = topClients(entrepriseId);
        List<DossierRentabiliteSummaryResponse> echantillon = calculerRentabiliteEchantillon(entrepriseId);
        List<DossierRentabiliteSummaryResponse> rentables = echantillon.stream()
                .sorted(Comparator.comparing(DossierRentabiliteSummaryResponse::margeEstimee).reversed())
                .limit(TOP_N).toList();
        List<DossierRentabiliteSummaryResponse> aRisque = echantillon.stream()
                .sorted(Comparator.comparing(DossierRentabiliteSummaryResponse::margeEstimee))
                .limit(TOP_N).toList();

        return new DashboardDirectionResponse(
                dossierRepository.countByStatut(entrepriseId).stream().mapToLong(DossierRepository.StatutCount::getTotal).sum(),
                totalFacture, totalEncaisse, totalEncaisse.subtract(totalCharges),
                tauxEncaissement(totalEncaisse, totalFacture), topClients, rentables, aRisque);
    }

    @Transactional(readOnly = true)
    public DashboardOperationsResponse operations() {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Map<String, Long> parStatut = dossiersParStatut(entrepriseId);
        LocalDate today = LocalDate.now();
        return new DashboardOperationsResponse(
                parStatut.values().stream().mapToLong(Long::longValue).sum(), parStatut,
                dossierRepository.countProchesEcheance(entrepriseId, today, today.plusDays(7)));
    }

    @Transactional(readOnly = true)
    public DashboardFacturationResponse facturation() {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        BigDecimal totalFacture = factureRepository.sumMontantTotalByEntreprise(entrepriseId);
        BigDecimal totalEncaisse = paiementRepository.sumValideByEntreprise(entrepriseId);
        LocalDate today = LocalDate.now();
        return new DashboardFacturationResponse(totalFacture, totalEncaisse,
                factureRepository.sumResteAPayerByEntreprise(entrepriseId), tauxEncaissement(totalEncaisse, totalFacture),
                factureRepository.countFacturesEnRetard(entrepriseId, today), factureRepository.sumMontantEnRetard(entrepriseId, today));
    }

    @Transactional(readOnly = true)
    public DashboardRecouvrementResponse recouvrement() {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        LocalDate today = LocalDate.now();
        Map<String, Long> relancesParStatut = relanceRepository.countByStatut(entrepriseId).stream()
                .collect(Collectors.toMap(RelanceRepository.StatutCount::getStatut, RelanceRepository.StatutCount::getTotal));
        return new DashboardRecouvrementResponse(
                factureRepository.countFacturesEnRetard(entrepriseId, today),
                factureRepository.sumMontantEnRetard(entrepriseId, today), relancesParStatut);
    }

    @Transactional(readOnly = true)
    public DashboardFinanceResponse finance() {
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        BigDecimal totalFacture = factureRepository.sumMontantTotalByEntreprise(entrepriseId);
        BigDecimal totalCharges = chargeRepository.sumByEntreprise(entrepriseId);
        BigDecimal totalEncaisse = paiementRepository.sumValideByEntreprise(entrepriseId);
        BigDecimal soldeCaisse = mouvementCaisseRepository.sumByType(entrepriseId, "ENTREE")
                .subtract(mouvementCaisseRepository.sumByType(entrepriseId, "SORTIE"));
        return new DashboardFinanceResponse(totalFacture, totalCharges, totalEncaisse, soldeCaisse,
                totalEncaisse.subtract(totalCharges));
    }

    private Map<String, Long> dossiersParStatut(UUID entrepriseId) {
        return dossierRepository.countByStatut(entrepriseId).stream()
                .collect(Collectors.toMap(DossierRepository.StatutCount::getStatut, DossierRepository.StatutCount::getTotal));
    }

    private BigDecimal tauxEncaissement(BigDecimal totalEncaisse, BigDecimal totalFacture) {
        if (totalFacture.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalEncaisse.multiply(BigDecimal.valueOf(100)).divide(totalFacture, 2, RoundingMode.HALF_UP);
    }

    private List<ClientMontantResponse> topClients(UUID entrepriseId) {
        List<FactureRepository.ClientTotal> totaux = factureRepository.topClients(entrepriseId, PageRequest.of(0, TOP_N));
        Map<UUID, Tiers> clients = tiersRepository.findAllById(totaux.stream().map(FactureRepository.ClientTotal::getClientId).toList())
                .stream().collect(Collectors.toMap(Tiers::getId, t -> t));
        return totaux.stream()
                .map(t -> new ClientMontantResponse(t.getClientId(),
                        clients.containsKey(t.getClientId()) ? clients.get(t.getClientId()).getRaisonSociale() : "Client inconnu",
                        t.getTotal()))
                .toList();
    }

    private List<DossierRentabiliteSummaryResponse> calculerRentabiliteEchantillon(UUID entrepriseId) {
        Pageable pageable = PageRequest.of(0, ECHANTILLON_RENTABILITE);
        List<Dossier> dossiers = dossierRepository.search(entrepriseId, null, null, null, null, pageable).getContent();
        return dossiers.stream()
                .map(d -> {
                    BigDecimal facture = factureRepository.sumMontantTotalByDossier(d.getId());
                    BigDecimal charges = chargeRepository.sumByDossier(d.getId());
                    return new DossierRentabiliteSummaryResponse(d.getId(), d.getNumero(), d.getTitre(), facture.subtract(charges));
                })
                .toList();
    }
}
