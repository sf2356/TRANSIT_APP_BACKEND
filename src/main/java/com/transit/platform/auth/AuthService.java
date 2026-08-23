package com.transit.platform.auth;

import com.transit.platform.auth.dto.*;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.entreprise.Entreprise;
import com.transit.platform.entreprise.EntrepriseRepository;
import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import com.transit.platform.role.Permission;
import com.transit.platform.role.Role;
import com.transit.platform.role.RoleRepository;
import com.transit.platform.security.JwtService;
import com.transit.platform.security.TenantContext;
import com.transit.platform.utilisateur.Utilisateur;
import com.transit.platform.utilisateur.UtilisateurRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ParametreEntrepriseRepository parametreRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantContext tenantContext;

    public AuthService(UtilisateurRepository utilisateurRepository, EntrepriseRepository entrepriseRepository,
                        ParametreEntrepriseRepository parametreRepository, RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService, TenantContext tenantContext) {
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.parametreRepository = parametreRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        entrepriseRepository.findAll().stream()
                .filter(e -> e.getEmail().equalsIgnoreCase(request.emailEntreprise()))
                .findAny()
                .ifPresent(e -> { throw BusinessException.conflict(ErrorCode.DUPLICATE_REFERENCE, "Une entreprise avec cet email existe déjà"); });

        Entreprise entreprise = new Entreprise();
        entreprise.setNom(request.nomEntreprise());
        entreprise.setEmail(request.emailEntreprise());
        entreprise.setDeviseDefaut("XOF");
        entreprise.setStatut("ACTIF");
        entreprise.setDateExpirationEssai(java.time.LocalDate.now().plusDays(14));
        entreprise = entrepriseRepository.save(entreprise);

        ParametreEntreprise parametres = new ParametreEntreprise();
        parametres.setEntrepriseId(entreprise.getId());
        parametres.setDevise("XOF");
        parametres.setConfigMetier(Map.of());
        parametreRepository.save(parametres);

        Role directeur = roleRepository.findAvailableForEntreprise(entreprise.getId()).stream()
                .filter(r -> "DIRECTEUR".equals(r.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Rôle système DIRECTEUR manquant — vérifier le seed V10"));

        Utilisateur admin = new Utilisateur();
        admin.setEntrepriseId(entreprise.getId());
        admin.setNom(request.nomAdmin());
        admin.setPrenom(request.prenomAdmin());
        admin.setEmail(request.emailAdmin());
        admin.setMotDePasseHash(passwordEncoder.encode(request.motDePasse()));
        admin.setStatut("ACTIF");
        admin.getRoles().add(directeur);
        admin = utilisateurRepository.save(admin);

        return issueTokens(admin);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(request.motDePasse(), utilisateur.getMotDePasseHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Identifiants invalides");
        }
        if (!"ACTIF".equals(utilisateur.getStatut())) {
            throw BusinessException.forbidden("Ce compte utilisateur est suspendu");
        }

        Entreprise entrepriseConnexion = entrepriseRepository.findById(utilisateur.getEntrepriseId()).orElseThrow();
        if (entrepriseConnexion.getDateExpirationEssai() != null
                && entrepriseConnexion.getDateExpirationEssai().isBefore(java.time.LocalDate.now())) {
            throw BusinessException.forbidden(
                    "Votre période d'essai de 14 jours est terminée. Contactez-nous pour continuer à utiliser la plateforme.");
        }

        utilisateur.setDerniereConnexion(Instant.now());
        utilisateurRepository.save(utilisateur);

        return issueTokens(utilisateur);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(request.refreshToken());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token invalide ou expiré", HttpStatus.UNAUTHORIZED);
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token fourni n'est pas un refresh token", HttpStatus.UNAUTHORIZED);
        }

        UUID utilisateurId = UUID.fromString(claims.get("uid", String.class));
        UUID entrepriseId = UUID.fromString(claims.get("eid", String.class));
        Utilisateur utilisateur = utilisateurRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(utilisateurId, entrepriseId)
                .filter(u -> "ACTIF".equals(u.getStatut()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Utilisateur introuvable ou inactif", HttpStatus.UNAUTHORIZED));

        return issueTokens(utilisateur);
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        UUID utilisateurId = tenantContext.currentUtilisateurId();
        UUID entrepriseId = tenantContext.currentEntrepriseId();
        Utilisateur utilisateur = utilisateurRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(utilisateurId, entrepriseId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.UTILISATEUR_NOT_FOUND, "Utilisateur introuvable"));
        return buildMeResponse(utilisateur);
    }

    /**
     * Modification du mot de passe par l'utilisateur lui-même (Paramètres). Toujours
     * vérifier l'ancien mot de passe avant d'accepter le nouveau — jamais de confiance sur
     * le seul fait que l'utilisateur soit déjà authentifié (un token volé ne doit pas
     * suffire à changer le mot de passe sans connaître l'ancien).
     */
    @Transactional
    public void changePassword(com.transit.platform.auth.dto.ChangePasswordRequest request) {
        UUID utilisateurId = tenantContext.currentUtilisateurId();
        com.transit.platform.utilisateur.Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.UTILISATEUR_NOT_FOUND, "Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.ancienMotDePasse(), utilisateur.getMotDePasseHash())) {
            throw BusinessException.unprocessable(ErrorCode.VALIDATION_NOT_FOUND, "Mot de passe actuel incorrect");
        }
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.nouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
    }

    private MeResponse buildMeResponse(Utilisateur utilisateur) {
        List<RoleResumeDto> roles = utilisateur.getRoles().stream()
                .map(r -> new RoleResumeDto(r.getId(), r.getCode(), r.getLibelle()))
                .toList();
        List<String> permissions = utilisateur.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .toList();
        Entreprise entreprise = entrepriseRepository.findById(utilisateur.getEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ENTREPRISE_NOT_FOUND, "Entreprise introuvable"));
        Long joursRestantsEssai = entreprise.getDateExpirationEssai() != null
                ? java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), entreprise.getDateExpirationEssai())
                : null;
        EntrepriseResumeDto entrepriseDto = new EntrepriseResumeDto(entreprise.getId(), entreprise.getNom(),
                entreprise.getDeviseDefaut(), entreprise.getLogo(), joursRestantsEssai);

        return new MeResponse(utilisateur.getId(), utilisateur.getNom(), utilisateur.getPrenom(), utilisateur.getEmail(),
                utilisateur.getTelephone(), roles, permissions, entrepriseDto, utilisateur.getVilleAffectation(),
                utilisateur.getStatut());
    }

    private TokenResponse issueTokens(Utilisateur utilisateur) {
        List<String> permissions = utilisateur.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .toList();

        String access = jwtService.generateAccessToken(utilisateur.getId(), utilisateur.getEntrepriseId(),
                utilisateur.getEmail(), permissions);
        String refresh = jwtService.generateRefreshToken(utilisateur.getId(), utilisateur.getEntrepriseId(), utilisateur.getEmail());
        return TokenResponse.of(access, refresh, jwtService.getAccessTokenExpirationSeconds(), buildMeResponse(utilisateur));
    }
}
