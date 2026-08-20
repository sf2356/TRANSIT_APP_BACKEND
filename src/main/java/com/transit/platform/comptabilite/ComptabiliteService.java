package com.transit.platform.comptabilite;

import com.transit.platform.caisse.MouvementCaisseRepository;
import com.transit.platform.charge.ChargeRepository;
import com.transit.platform.comptabilite.dto.ComptabiliteOperationnelleResponse;
import com.transit.platform.facture.FactureRepository;
import com.transit.platform.paiement.PaiementRepository;
import com.transit.platform.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ComptabiliteService {

    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final ChargeRepository chargeRepository;
    private final MouvementCaisseRepository mouvementCaisseRepository;
    private final TenantContext tenantContext;

    public ComptabiliteService(FactureRepository factureRepository, PaiementRepository paiementRepository,
                                ChargeRepository chargeRepository, MouvementCaisseRepository mouvementCaisseRepository,
                                TenantContext tenantContext) {
        this.factureRepository = factureRepository;
        this.paiementRepository = paiementRepository;
        this.chargeRepository = chargeRepository;
        this.mouvementCaisseRepository = mouvementCaisseRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public ComptabiliteOperationnelleResponse operationnelle(UUID clientId, UUID dossierId, LocalDate dateDebut, LocalDate dateFin) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();

        // Correctif (session de test réelle) : ne jamais transmettre null à une requête castée
        // explicitement — voir CaisseService pour l'explication complète du bug PostgreSQL/JDBC.
        LocalDate dateDebutEffective = dateDebut != null ? dateDebut : LocalDate.of(1900, 1, 1);
        LocalDate dateFinEffective = dateFin != null ? dateFin : LocalDate.of(2100, 12, 31);
        Instant instantDebut = dateDebutEffective.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant instantFin = dateFinEffective.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        var totalFacture = factureRepository.sumMontantTotalFiltre(entrepriseId, clientId, dossierId, dateDebutEffective, dateFinEffective);
        var totalEncaisse = paiementRepository.sumValideFiltre(entrepriseId, clientId, dossierId, dateDebutEffective, dateFinEffective);
        var reste = factureRepository.sumResteAPayerFiltre(entrepriseId, clientId, dossierId, dateDebutEffective, dateFinEffective);
        var charges = chargeRepository.sumFiltre(entrepriseId, dossierId, dateDebutEffective, dateFinEffective);
        var entrees = mouvementCaisseRepository.sumByTypeFiltre(entrepriseId, "ENTREE", dossierId, instantDebut, instantFin);
        var sorties = mouvementCaisseRepository.sumByTypeFiltre(entrepriseId, "SORTIE", dossierId, instantDebut, instantFin);
        return new ComptabiliteOperationnelleResponse(totalFacture, totalEncaisse, reste, charges,
                entrees, sorties, entrees.subtract(sorties), totalEncaisse.subtract(charges));
    }
}
