package com.transit.platform.paiement;

import com.transit.platform.common.BusinessException;
import com.transit.platform.dossier.Dossier;
import com.transit.platform.dossier.DossierRepository;
import com.transit.platform.entreprise.Entreprise;
import com.transit.platform.entreprise.EntrepriseRepository;
import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import com.transit.platform.facture.Facture;
import com.transit.platform.facture.FactureRepository;
import com.transit.platform.facture.LigneFacture;
import com.transit.platform.facture.LigneFactureRepository;
import com.transit.platform.paiement.dto.CreatePaiementRequest;
import com.transit.platform.reference.ReferenceGeneratorService;
import com.transit.platform.reference.ReferenceType;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cas critiques n°3, 4, 5 du Prompt 03 §47 : paiement partiel -> PARTIELLEMENT_PAYEE,
 * paiement total -> PAYEE, et rejet d'un paiement dépassant le reste à payer (§26/§32).
 */
@SpringBootTest
@ActiveProfiles("test")
class PaiementFactureStatutIT {

    @Autowired private PaiementService paiementService;
    @Autowired private EntrepriseRepository entrepriseRepository;
    @Autowired private ParametreEntrepriseRepository parametreRepository;
    @Autowired private TiersRepository tiersRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private DossierRepository dossierRepository;
    @Autowired private FactureRepository factureRepository;
    @Autowired private LigneFactureRepository ligneFactureRepository;
    @Autowired private ReferenceGeneratorService referenceGeneratorService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void paiementPartiel_puisTotal_mettentAJourLeStatutDeLaFacture() {
        UUID entrepriseId = creerEntreprise();
        UUID utilisateurId = creerUtilisateur(entrepriseId);
        authentifierComme(utilisateurId, entrepriseId);

        UUID clientId = creerTiers(entrepriseId);
        Facture facture = creerFacture(entrepriseId, clientId, utilisateurId, new BigDecimal("100000.00"));

        // Paiement partiel
        paiementService.create(new CreatePaiementRequest(facture.getId(), null, null, null,
                new BigDecimal("40000.00"), "XOF", "VIREMENT", null, null, null), null);

        Facture apresPartiel = factureRepository.findById(facture.getId()).orElseThrow();
        assertThat(apresPartiel.getStatut()).isEqualTo("PARTIELLEMENT_PAYEE");
        assertThat(apresPartiel.getMontantPaye()).isEqualByComparingTo("40000.00");
        assertThat(apresPartiel.getResteAPayer()).isEqualByComparingTo("60000.00");

        // Paiement du solde -> PAYEE
        paiementService.create(new CreatePaiementRequest(facture.getId(), null, null, null,
                new BigDecimal("60000.00"), "XOF", "VIREMENT", null, null, null), null);

        Facture apresTotal = factureRepository.findById(facture.getId()).orElseThrow();
        assertThat(apresTotal.getStatut()).isEqualTo("PAYEE");
        assertThat(apresTotal.getResteAPayer()).isEqualByComparingTo("0.00");
    }

    @Test
    void paiementDepassantLeResteAPayer_estRejete() {
        UUID entrepriseId = creerEntreprise();
        UUID utilisateurId = creerUtilisateur(entrepriseId);
        authentifierComme(utilisateurId, entrepriseId);

        UUID clientId = creerTiers(entrepriseId);
        Facture facture = creerFacture(entrepriseId, clientId, utilisateurId, new BigDecimal("50000.00"));

        assertThatThrownBy(() -> paiementService.create(new CreatePaiementRequest(facture.getId(), null, null, null,
                new BigDecimal("999999.00"), "XOF", "VIREMENT", null, null, null), null))
                .isInstanceOf(BusinessException.class);
    }

    /** Prompt 04 §41-42 : une double soumission avec la même Idempotency-Key ne doit créer qu'un seul paiement. */
    @Test
    void doubleSoumissionAvecMemeIdempotencyKey_neCreeQuUnSeulPaiement() {
        UUID entrepriseId = creerEntreprise();
        UUID utilisateurId = creerUtilisateur(entrepriseId);
        authentifierComme(utilisateurId, entrepriseId);

        UUID clientId = creerTiers(entrepriseId);
        Facture facture = creerFacture(entrepriseId, clientId, utilisateurId, new BigDecimal("100000.00"));
        String idempotencyKey = "test-double-clic-" + UUID.randomUUID();

        var premierePaiement = paiementService.create(new CreatePaiementRequest(facture.getId(), null, null, null,
                new BigDecimal("50000.00"), "XOF", "VIREMENT", null, null, null), idempotencyKey);

        // Double clic : même clé, même requête envoyée une seconde fois.
        var deuxiemeAppel = paiementService.create(new CreatePaiementRequest(facture.getId(), null, null, null,
                new BigDecimal("50000.00"), "XOF", "VIREMENT", null, null, null), idempotencyKey);

        assertThat(deuxiemeAppel.id()).isEqualTo(premierePaiement.id());

        Facture apres = factureRepository.findById(facture.getId()).orElseThrow();
        // Un seul paiement de 50000 a réellement été enregistré, pas deux (montantPaye != 100000).
        assertThat(apres.getMontantPaye()).isEqualByComparingTo("50000.00");
    }

    private Facture creerFacture(UUID entrepriseId, UUID clientId, UUID createdBy, BigDecimal montantTotal) {
        Facture facture = new Facture();
        facture.setEntrepriseId(entrepriseId);
        facture.setNumero(referenceGeneratorService.generate(entrepriseId, ReferenceType.FACTURE));
        facture.setClientId(clientId);
        facture.setDevise("XOF");
        facture.setStatut("EMISE");
        facture.setCreatedBy(createdBy);
        facture.setMontantHT(montantTotal);
        facture.setMontantTotal(montantTotal);
        facture.setResteAPayer(montantTotal);
        facture = factureRepository.save(facture);

        LigneFacture ligne = new LigneFacture();
        ligne.setFactureId(facture.getId());
        ligne.setDescription("Prestation");
        ligne.setQuantite(BigDecimal.ONE);
        ligne.setPrixUnitaire(montantTotal);
        ligne.setMontant(montantTotal);
        ligneFactureRepository.save(ligne);

        return facture;
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
