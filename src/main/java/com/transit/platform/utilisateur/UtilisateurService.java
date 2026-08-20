package com.transit.platform.utilisateur;

import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.role.Role;
import com.transit.platform.role.RoleRepository;
import com.transit.platform.security.TenantContext;
import com.transit.platform.utilisateur.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder, TenantContext tenantContext) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public Page<UtilisateurResponse> search(String search, Pageable pageable) {
        String normalized = search == null ? null : "%" + search.toLowerCase() + "%";
        return utilisateurRepository.search(tenantContext.currentEntrepriseId(), normalized, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse getById(UUID id) {
        return toResponse(findByIdWithinTenant(id));
    }

    @Transactional
    public UtilisateurResponse create(CreateUtilisateurRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();

        utilisateurRepository.findByEmailAndEntrepriseIdAndDeletedAtIsNull(request.email(), entrepriseId)
                .ifPresent(u -> {
                    throw BusinessException.conflict(ErrorCode.DUPLICATE_REFERENCE, "Un utilisateur avec cet email existe déjà");
                });

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEntrepriseId(entrepriseId);
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());
        utilisateur.setEmail(request.email());
        utilisateur.setTelephone(request.telephone());
        utilisateur.setVilleAffectation(request.villeAffectation());
        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.motDePasse()));
        utilisateur.setStatut("ACTIF");
        utilisateur.getRoles().addAll(resolveRoles(request.roleCodes(), entrepriseId));

        return toResponse(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public UtilisateurResponse update(UUID id, UpdateUtilisateurRequest request) {
        Utilisateur utilisateur = findByIdWithinTenant(id);
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());
        utilisateur.setTelephone(request.telephone());
        utilisateur.setVilleAffectation(request.villeAffectation());
        if (request.roleCodes() != null && !request.roleCodes().isEmpty()) {
            utilisateur.getRoles().clear();
            utilisateur.getRoles().addAll(resolveRoles(request.roleCodes(), utilisateur.getEntrepriseId()));
        }
        return toResponse(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public void suspend(UUID id) {
        Utilisateur utilisateur = findByIdWithinTenant(id);
        utilisateur.setStatut("SUSPENDU");
        utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public void restore(UUID id) {
        Utilisateur utilisateur = findByIdWithinTenant(id);
        utilisateur.setStatut("ACTIF");
        utilisateurRepository.save(utilisateur);
    }

    /**
     * Toute lecture/écriture repasse systématiquement par l'entreprise du contexte courant :
     * un utilisateur d'une autre entreprise renvoie 404, jamais les données (protection IDOR).
     */
    private Utilisateur findByIdWithinTenant(UUID id) {
        return utilisateurRepository.findByIdAndEntrepriseIdAndDeletedAtIsNull(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.UTILISATEUR_NOT_FOUND, "Utilisateur introuvable"));
    }

    private Set<Role> resolveRoles(List<String> codes, UUID entrepriseId) {
        List<Role> disponibles = roleRepository.findAvailableForEntreprise(entrepriseId);
        Set<Role> resolved = disponibles.stream().filter(r -> codes.contains(r.getCode())).collect(Collectors.toSet());
        if (resolved.size() != codes.size()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR, "Un ou plusieurs rôles indiqués sont invalides");
        }
        return resolved;
    }

    private UtilisateurResponse toResponse(Utilisateur u) {
        List<String> roles = u.getRoles().stream().map(Role::getCode).toList();
        return new UtilisateurResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
                u.getVilleAffectation(), u.getStatut(), u.getDerniereConnexion(), roles);
    }
}
