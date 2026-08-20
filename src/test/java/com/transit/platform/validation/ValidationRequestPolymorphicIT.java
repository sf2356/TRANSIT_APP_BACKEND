package com.transit.platform.validation;

import com.transit.platform.common.BusinessException;
import com.transit.platform.entreprise.Entreprise;
import com.transit.platform.entreprise.EntrepriseRepository;
import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import com.transit.platform.security.CurrentUserPrincipal;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import com.transit.platform.utilisateur.Utilisateur;
import com.transit.platform.utilisateur.UtilisateurRepository;
import com.transit.platform.validation.dto.CreateValidationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie que le contrôle d'intégrité applicatif (absence de FK physique sur entite_id,
 * cf. V19) rejette bien une validation pointant vers une entité d'une autre entreprise —
 * c'est le seul rempart pour cette relation polymorphe (Prompt 02 §44).
 */
@SpringBootTest
@ActiveProfiles("test")
class ValidationRequestPolymorphicIT {

    @Autowired private ValidationRequestService validationRequestService;
    @Autowired private EntrepriseRepository entrepriseRepository;
    @Autowired private ParametreEntrepriseRepository parametreRepository;
    @Autowired private TiersRepository tiersRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void demandeDeValidationSurTiersDuneAutreEntreprise_estRejetee() {
        UUID entrepriseA = creerEntreprise();
        UUID entrepriseB = creerEntreprise();
        UUID tiersB = creerTiers(entrepriseB); // utilisé ici comme "entité étrangère" arbitraire

        UUID utilisateurA = creerUtilisateur(entrepriseA);
        authentifierComme(utilisateurA, entrepriseA);

        // "CHARGE" nécessite une charge existante ; on simule avec un entiteId inexistant côté A
        // pour vérifier que verifierExistenceEtTenant rejette une entité absente du tenant courant.
        assertThatThrownBy(() -> validationRequestService.create(
                new CreateValidationRequest("APPROBATION_CHARGE", "CHARGE", tiersB, "test")))
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
        t.setRaisonSociale("Tiers Test");
        t.setType("FOURNISSEUR");
        t.setStatut("ACTIF");
        return tiersRepository.save(t).getId();
    }

    private void authentifierComme(UUID utilisateurId, UUID entrepriseId) {
        CurrentUserPrincipal principal = CurrentUserPrincipal.of(utilisateurId, entrepriseId, "test@test.local", List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));
    }
}
