package com.transit.platform.dossier;

import com.transit.platform.common.BusinessException;
import com.transit.platform.entreprise.Entreprise;
import com.transit.platform.entreprise.EntrepriseRepository;
import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import com.transit.platform.reference.ReferenceGeneratorService;
import com.transit.platform.reference.ReferenceType;
import com.transit.platform.security.CurrentUserPrincipal;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cas critique n°1 du Prompt 03 §47 : un utilisateur de l'entreprise A ne doit jamais
 * pouvoir accéder à un dossier de l'entreprise B, même en connaissant son UUID.
 */
@SpringBootTest
@ActiveProfiles("test")
class DossierMultiTenantIsolationIT {

    @Autowired private DossierService dossierService;
    @Autowired private EntrepriseRepository entrepriseRepository;
    @Autowired private ParametreEntrepriseRepository parametreRepository;
    @Autowired private TiersRepository tiersRepository;
    @Autowired private ReferenceGeneratorService referenceGeneratorService;
    @Autowired private DossierRepository dossierRepository;

    @Test
    void utilisateurEntrepriseA_neDoitJamaisAccederAuDossierDeEntrepriseB() {
        UUID entrepriseA = creerEntreprise();
        UUID entrepriseB = creerEntreprise();

        UUID clientB = creerTiers(entrepriseB);
        authentifierComme(UUID.randomUUID(), entrepriseB);
        String numeroDossierB = referenceGeneratorService.generate(entrepriseB, ReferenceType.DOSSIER);

        Dossier dossierB = new Dossier();
        dossierB.setEntrepriseId(entrepriseB);
        dossierB.setNumero(numeroDossierB);
        dossierB.setClientId(clientB);
        dossierB.setTitre("Dossier confidentiel entreprise B");
        dossierB.setStatut("OUVERT");
        dossierB = dossierRepository.save(dossierB);

        // Un utilisateur authentifié pour l'entreprise A tente d'accéder au dossier de B
        authentifierComme(UUID.randomUUID(), entrepriseA);

        assertThatThrownBy(() -> dossierService.getById(dossierB.getId()))
                .isInstanceOf(BusinessException.class);
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
        SecurityContext context = new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
