package com.transit.platform.config;

import com.transit.platform.caisse.MouvementCaisse;
import com.transit.platform.caisse.MouvementCaisseRepository;
import com.transit.platform.charge.Charge;
import com.transit.platform.charge.ChargeRepository;
import com.transit.platform.cotation.Cotation;
import com.transit.platform.cotation.CotationRepository;
import com.transit.platform.cotation.LigneCotation;
import com.transit.platform.cotation.LigneCotationRepository;
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
import com.transit.platform.marchandise.Marchandise;
import com.transit.platform.marchandise.MarchandiseRepository;
import com.transit.platform.paiement.Paiement;
import com.transit.platform.paiement.PaiementRepository;
import com.transit.platform.reference.ReferenceGeneratorService;
import com.transit.platform.reference.ReferenceType;
import com.transit.platform.role.Role;
import com.transit.platform.role.RoleRepository;
import com.transit.platform.tiers.Tiers;
import com.transit.platform.tiers.TiersRepository;
import com.transit.platform.utilisateur.Utilisateur;
import com.transit.platform.utilisateur.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Données de démonstration (Prompt 02 §16, Prompt 03 §48) — profil dev/staging uniquement,
 * jamais exécuté en production (activation conditionnée par app.seed.enabled, absent des
 * profils prod).
 *
 * NOTE D'ÉTAPE : ce seeder couvre les modules livrés à ce stade (Entreprise, Utilisateurs,
 * Rôles, Tiers, Dossier). Marchandises/Cotations/Factures/Paiements/Charges seront ajoutés
 * au seeder au fur et à mesure de leur implémentation (étapes 10 à 16), pour rester
 * cohérent avec l'ordre d'implémentation du Prompt 03 §59.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final EntrepriseRepository entrepriseRepository;
    private final ParametreEntrepriseRepository parametreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final TiersRepository tiersRepository;
    private final DossierRepository dossierRepository;
    private final MarchandiseRepository marchandiseRepository;
    private final CotationRepository cotationRepository;
    private final LigneCotationRepository ligneCotationRepository;
    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final PaiementRepository paiementRepository;
    private final ChargeRepository chargeRepository;
    private final ReferenceGeneratorService referenceGeneratorService;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(EntrepriseRepository entrepriseRepository, ParametreEntrepriseRepository parametreRepository,
                          UtilisateurRepository utilisateurRepository, RoleRepository roleRepository,
                          TiersRepository tiersRepository, DossierRepository dossierRepository,
                          MarchandiseRepository marchandiseRepository, CotationRepository cotationRepository,
                          LigneCotationRepository ligneCotationRepository, FactureRepository factureRepository,
                          LigneFactureRepository ligneFactureRepository, PaiementRepository paiementRepository,
                          ChargeRepository chargeRepository, ReferenceGeneratorService referenceGeneratorService,
                          PasswordEncoder passwordEncoder) {
        this.entrepriseRepository = entrepriseRepository;
        this.parametreRepository = parametreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.tiersRepository = tiersRepository;
        this.dossierRepository = dossierRepository;
        this.marchandiseRepository = marchandiseRepository;
        this.cotationRepository = cotationRepository;
        this.ligneCotationRepository = ligneCotationRepository;
        this.factureRepository = factureRepository;
        this.ligneFactureRepository = ligneFactureRepository;
        this.paiementRepository = paiementRepository;
        this.chargeRepository = chargeRepository;
        this.referenceGeneratorService = referenceGeneratorService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (entrepriseRepository.count() > 0) {
            return; // déjà initialisé (redémarrage) — ne jamais re-semer
        }

        Entreprise entreprise = new Entreprise();
        entreprise.setNom("Entreprise Transit Demo");
        entreprise.setEmail("contact@transit-demo.test");
        entreprise.setDeviseDefaut("XOF");
        entreprise.setPays("Burkina Faso");
        entreprise.setVille("Ouagadougou");
        entreprise.setStatut("ACTIF");
        entreprise = entrepriseRepository.save(entreprise);

        ParametreEntreprise parametres = new ParametreEntreprise();
        parametres.setEntrepriseId(entreprise.getId());
        parametres.setDevise("XOF");
        parametres.setConfigMetier(Map.of());
        parametreRepository.save(parametres);

        List<Role> rolesSysteme = roleRepository.findAvailableForEntreprise(entreprise.getId());
        Role directeur = roleByCode(rolesSysteme, "DIRECTEUR");
        Role comptable = roleByCode(rolesSysteme, "COMPTABLE");
        Role agentTransit = roleByCode(rolesSysteme, "AGENT_TRANSIT");
        Role commercial = roleByCode(rolesSysteme, "COMMERCIAL");
        Role responsableLogistique = roleByCode(rolesSysteme, "RESPONSABLE_LOGISTIQUE");

        Utilisateur admin = creerUtilisateur(entreprise.getId(), "Admin", "Administrateur", "admin@transit-demo.test", directeur);
        creerUtilisateur(entreprise.getId(), "Traore", "Fatou", "comptable@transit-demo.test", comptable);
        Utilisateur agent = creerUtilisateur(entreprise.getId(), "Ouedraogo", "Issa", "agent@transit-demo.test", agentTransit);
        creerUtilisateur(entreprise.getId(), "Sawadogo", "Aminata", "commercial@transit-demo.test", commercial);
        creerUtilisateur(entreprise.getId(), "Kabore", "Boureima", "logistique@transit-demo.test", responsableLogistique);

        Tiers client1 = creerTiers(entreprise.getId(), "MAINTECH FASO SARL", "CLIENT");
        creerTiers(entreprise.getId(), "SONATRA TRANSIT", "CLIENT");

        Dossier dossier1 = creerDossier(entreprise.getId(), client1.getId(), agent.getId(), "Dédouanement de pièces de forage");
        creerDossier(entreprise.getId(), client1.getId(), agent.getId(), "Import matériel industriel");

        Marchandise marchandise = new Marchandise();
        marchandise.setDossierId(dossier1.getId());
        marchandise.setDesignation("Pièces de forage");
        marchandise.setTypeMarchandise("GENERALE");
        marchandise.setStatut("DECLAREE");
        marchandise.setNombreColis(12);
        marchandise.setPoidsBrut(new BigDecimal("3200.500"));
        marchandiseRepository.save(marchandise);

        Cotation cotation = new Cotation();
        cotation.setEntrepriseId(entreprise.getId());
        cotation.setNumero(referenceGeneratorService.generate(entreprise.getId(), ReferenceType.COTATION));
        cotation.setClientId(client1.getId());
        cotation.setDossierId(dossier1.getId());
        cotation.setTitre("Cotation dédouanement pièces de forage");
        cotation.setDevise("XOF");
        cotation.setStatut("BROUILLON");
        cotation.setCreatedBy(admin.getId());
        cotation = cotationRepository.save(cotation);

        LigneCotation ligne1 = new LigneCotation();
        ligne1.setCotationId(cotation.getId());
        ligne1.setDescription("Frais de transit");
        ligne1.setCategorieFrais("FRAIS_TRANSIT");
        ligne1.setQuantite(BigDecimal.ONE);
        ligne1.setPrixUnitaire(new BigDecimal("250000"));
        ligne1.setTauxTaxe(new BigDecimal("18"));
        ligne1.setMontant(new BigDecimal("250000.00"));
        ligne1.setMontantTaxe(new BigDecimal("45000.00"));
        ligneCotationRepository.save(ligne1);

        cotation.setMontantHT(new BigDecimal("250000.00"));
        cotation.setMontantTaxe(new BigDecimal("45000.00"));
        cotation.setMontantTotal(new BigDecimal("295000.00"));
        cotationRepository.save(cotation);

        Facture facture = new Facture();
        facture.setEntrepriseId(entreprise.getId());
        facture.setNumero(referenceGeneratorService.generate(entreprise.getId(), ReferenceType.FACTURE));
        facture.setTypeDocument("FACTURE");
        facture.setClientId(client1.getId());
        facture.setDossierId(dossier1.getId());
        facture.setCotationId(cotation.getId());
        facture.setTitre("Facture dédouanement pièces de forage");
        facture.setDevise("XOF");
        facture.setStatut("EMISE");
        facture.setCreatedBy(admin.getId());
        facture = factureRepository.save(facture);

        LigneFacture ligneFacture = new LigneFacture();
        ligneFacture.setFactureId(facture.getId());
        ligneFacture.setDescription("Frais de transit");
        ligneFacture.setCategorieFrais("FRAIS_TRANSIT");
        ligneFacture.setQuantite(BigDecimal.ONE);
        ligneFacture.setPrixUnitaire(new BigDecimal("250000"));
        ligneFacture.setTauxTaxe(new BigDecimal("18"));
        ligneFacture.setMontant(new BigDecimal("250000.00"));
        ligneFacture.setMontantTaxe(new BigDecimal("45000.00"));
        ligneFactureRepository.save(ligneFacture);

        facture.setMontantHT(new BigDecimal("250000.00"));
        facture.setMontantTaxe(new BigDecimal("45000.00"));
        facture.setMontantTotal(new BigDecimal("295000.00"));
        facture.setResteAPayer(new BigDecimal("295000.00"));
        facture = factureRepository.save(facture);

        Paiement paiement = new Paiement();
        paiement.setEntrepriseId(entreprise.getId());
        paiement.setNumero(referenceGeneratorService.generate(entreprise.getId(), ReferenceType.PAIEMENT));
        paiement.setFactureId(facture.getId());
        paiement.setDossierId(dossier1.getId());
        paiement.setClientId(client1.getId());
        paiement.setMontant(new BigDecimal("150000.00"));
        paiement.setDevise("XOF");
        paiement.setModePaiement("VIREMENT");
        paiement.setStatut("VALIDE");
        paiement.setCreatedBy(admin.getId());
        paiementRepository.save(paiement);

        facture.setMontantPaye(new BigDecimal("150000.00"));
        facture.setResteAPayer(new BigDecimal("145000.00"));
        facture.setStatut("PARTIELLEMENT_PAYEE");
        facture = factureRepository.save(facture);

        Charge charge = new Charge();
        charge.setEntrepriseId(entreprise.getId());
        charge.setDossierId(dossier1.getId());
        charge.setLibelle("Droit de douane");
        charge.setType("DROITS_TAXES");
        charge.setMontant(new BigDecimal("80000.00"));
        charge.setDevise("XOF");
        charge.setStatut("VALIDEE");
        charge.setCreatedBy(admin.getId());
        chargeRepository.save(charge);
    }

    private Role roleByCode(List<Role> roles, String code) {
        return roles.stream().filter(r -> r.getCode().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Rôle système manquant : " + code));
    }

    private Utilisateur creerUtilisateur(java.util.UUID entrepriseId, String nom, String prenom, String email, Role role) {
        Utilisateur u = new Utilisateur();
        u.setEntrepriseId(entrepriseId);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setMotDePasseHash(passwordEncoder.encode("Demo1234!"));
        u.setStatut("ACTIF");
        u.getRoles().add(role);
        return utilisateurRepository.save(u);
    }

    private Tiers creerTiers(java.util.UUID entrepriseId, String raisonSociale, String type) {
        Tiers t = new Tiers();
        t.setEntrepriseId(entrepriseId);
        t.setRaisonSociale(raisonSociale);
        t.setType(type);
        t.setStatut("ACTIF");
        return tiersRepository.save(t);
    }

    private Dossier creerDossier(java.util.UUID entrepriseId, java.util.UUID clientId, java.util.UUID responsableId, String titre) {
        Dossier d = new Dossier();
        d.setEntrepriseId(entrepriseId);
        d.setNumero(referenceGeneratorService.generate(entrepriseId, ReferenceType.DOSSIER));
        d.setClientId(clientId);
        d.setResponsableId(responsableId);
        d.setTitre(titre);
        d.setStatut("OUVERT");
        return dossierRepository.save(d);
    }
}
