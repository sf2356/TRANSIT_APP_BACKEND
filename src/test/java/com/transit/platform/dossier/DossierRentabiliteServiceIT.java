package com.transit.platform.dossier;

import com.transit.platform.charge.Charge;
import com.transit.platform.charge.ChargeRepository;
import com.transit.platform.dossier.dto.DossierRentabiliteResponse;
import com.transit.platform.entreprise.Entreprise;
import com.transit.platform.entreprise.EntrepriseRepository;
import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import com.transit.platform.facture.Facture;
import com.transit.platform.facture.FactureRepository;
import com.transit.platform.paiement.Paiement;
import com.transit.platform.paiement.PaiementRepository;
import com.transit.platform.security.CurrentUserPrincipal;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import com.transit.platform.utilisateur.Utilisateur;
import com.transit.platform.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie l'exemple donné au Prompt 03 §32 : un dossier encaissé au-delà du facturé
 * (paiement > facture, ex. avance ou double règlement) doit produire un resteAEncaisser
 * négatif SANS lever d'erreur — la marge reste calculée sur l'encaissé réel, pas sur une
 * valeur tronquée arbitrairement.
 */
@SpringBootTest
@ActiveProfiles("test")
class DossierRentabiliteServiceIT {

    @Autowired private DossierRentabiliteService rentabiliteService;
    @Autowired private DossierRepository dossierRepository;
    @Autowired private FactureRepository factureRepository;
    @Autowired private PaiementRepository paiementRepository;
    @Autowired private ChargeRepository chargeRepository;
    @Autowired private EntrepriseRepository entrepriseRepository;
    @Autowired private ParametreEntrepriseRepository parametreRepository;
    @Autowired private TiersRepository tiersRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void rentabilite_gereCorrectementUnEncaissementSuperieurAuFacture() {
        UUID entrepriseId = creerEntreprise();
        UUID utilisateurId = creerUtilisateur(entrepriseId);
        authentifierComme(utilisateurId, entrepriseId);
        UUID clientId = creerTiers(entrepriseId);

        Dossier dossier = new Dossier();
        dossier.setEntrepriseId(entrepriseId);
        dossier.setNumero("DOS-TEST-" + UUID.randomUUID());
        dossier.setClientId(clientId);
        dossier.setTitre("Dossier test rentabilité");
        dossier.setStatut("EN_COURS");
        dossier = dossierRepository.save(dossier);

        Facture facture = new Facture();
        facture.setEntrepriseId(entrepriseId);
        facture.setNumero("FAC-TEST-" + UUID.randomUUID());
        facture.setClientId(clientId);
        facture.setDossierId(dossier.getId());
        facture.setDevise("XOF");
        facture.setStatut("PAYEE");
        facture.setCreatedBy(utilisateurId);
        facture.setMontantTotal(new BigDecimal("3000000"));
        facture.setResteAPayer(BigDecimal.ZERO);
        facture = factureRepository.save(facture);

        Paiement paiement = new Paiement();
        paiement.setEntrepriseId(entrepriseId);
        paiement.setNumero("PAY-TEST-" + UUID.randomUUID());
        paiement.setFactureId(facture.getId());
        paiement.setDossierId(dossier.getId());
        paiement.setClientId(clientId);
        paiement.setMontant(new BigDecimal("3300000")); // > montant facturé, cf. exemple §32
        paiement.setDevise("XOF");
        paiement.setModePaiement("VIREMENT");
        paiement.setStatut("VALIDE");
        paiement.setCreatedBy(utilisateurId);
        paiementRepository.save(paiement);

        Charge charge = new Charge();
        charge.setEntrepriseId(entrepriseId);
        charge.setDossierId(dossier.getId());
        charge.setLibelle("Droits et taxes");
        charge.setType("DROITS_TAXES");
        charge.setMontant(new BigDecimal("2600000"));
        charge.setDevise("XOF");
        charge.setStatut("VALIDEE");
        charge.setCreatedBy(utilisateurId);
        chargeRepository.save(charge);

        DossierRentabiliteResponse rentabilite = rentabiliteService.calculer(dossier.getId());

        assertThat(rentabilite.totalFacture()).isEqualByComparingTo("3000000");
        assertThat(rentabilite.totalEncaisse()).isEqualByComparingTo("3300000");
        assertThat(rentabilite.resteAEncaisser()).isEqualByComparingTo("-300000"); // négatif, assumé (cf. §32)
        assertThat(rentabilite.totalCharges()).isEqualByComparingTo("2600000");
        assertThat(rentabilite.margeEstimee()).isEqualByComparingTo("700000"); // 3300000 - 2600000
    }

    private UUID creerEntreprise() {
        Entreprise e = new Entreprise();
        e.setNom("Entreprise " + UUID.randomUUID());
        e.setEmail(UUID.randomUUID() + "@test.local");
        e.setDeviseDefaut("XOF");
        e.setStatut("ACTIF");
        e = entrepriseRepository.save(e);

        ParametreEntreprise p = new ParametreEntreprise();
        p.setEntrepriseId(e.getId());
        p.setDevise("XOF");
        p.setConfigMetier(Map.of());
        parametreRepository.save(p);
        return e.getId();
    }

    private UUID creerUtilisateur(UUID entrepriseId) {
        Utilisateur u = new Utilisateur();
        u.setEntrepriseId(entrepriseId);
        u.setNom("Test");
        u.setPrenom("Utilisateur");
        u.setEmail(UUID.randomUUID() + "@test.local");
        u.setMotDePasseHash(passwordEncoder.encode("Test1234!"));
        u.setStatut("ACTIF");
        return utilisateurRepository.save(u).getId();
    }

    private UUID creerTiers(UUID entrepriseId) {
        Tiers t = new Tiers();
        t.setEntrepriseId(entrepriseId);
        t.setRaisonSociale("Client Test");
        t.setType("CLIENT");
        t.setStatut("ACTIF");
        return tiersRepository.save(t).getId();
    }

    private void authentifierComme(UUID utilisateurId, UUID entrepriseId) {
        CurrentUserPrincipal principal = CurrentUserPrincipal.of(utilisateurId, entrepriseId, "test@test.local", List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));
    }
}
