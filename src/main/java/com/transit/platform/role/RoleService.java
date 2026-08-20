package com.transit.platform.role;

import com.transit.platform.audit.AuditService;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import com.transit.platform.role.dto.*;
import com.transit.platform.security.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * IMPORTANT — limite architecturale assumée et documentée (voir README, section "Points
 * signalés Prompt 04") : les rôles système (DIRECTEUR, etc.) sont des lignes PARTAGÉES entre
 * toutes les entreprises (entreprise_id NULL, cf. Prompt 02/03). Modifier leurs permissions
 * via cette API changerait le comportement de TOUTES les entreprises simultanément — c'est
 * un défaut de conception à corriger, pas une fonctionnalité. En attendant un correctif du
 * modèle (ex. dupliquer les rôles système par entreprise à la création du tenant), cette
 * classe REFUSE explicitement toute modification d'un rôle système plutôt que de risquer
 * une fuite de configuration inter-tenant.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                        TenantContext tenantContext, AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listAvailable() {
        return roleRepository.findAvailableForEntreprise(tenantContext.currentEntrepriseId()).stream()
                .map(r -> toResponse(r, tenantContext.currentEntrepriseId())).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getId(), p.getCode(), p.getModule(), p.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getPermissionCodes(UUID roleId) {
        Role role = roleRepository.findByIdAccessibleForEntreprise(roleId, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ROLE_NOT_FOUND, "Rôle introuvable"));
        return role.getPermissions().stream().map(Permission::getCode).toList();
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        UUID entrepriseId = tenantContext.currentEntrepriseId();

        Role role = new Role();
        role.setEntrepriseId(entrepriseId);
        role.setCode(request.code().toUpperCase());
        role.setLibelle(request.libelle());
        role.setEstSysteme(false);
        if (request.permissionCodes() != null && !request.permissionCodes().isEmpty()) {
            role.getPermissions().addAll(resolvePermissions(request.permissionCodes()));
        }

        try {
            role = roleRepository.save(role);
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict(ErrorCode.DUPLICATE_REFERENCE,
                    "Un rôle avec ce code existe déjà pour cette entreprise");
        }

        auditService.log("CREATE", "ROLE", role.getId(), null, Map.of("code", role.getCode()));
        return toResponse(role, entrepriseId);
    }

    @Transactional
    public RoleResponse update(UUID id, UpdateRoleRequest request) {
        Role role = findRoleModifiable(id);
        role.setLibelle(request.libelle());
        role = roleRepository.save(role);
        return toResponse(role, tenantContext.currentEntrepriseId());
    }

    @Transactional
    public List<String> updatePermissions(UUID id, UpdateRolePermissionsRequest request) {
        Role role = findRoleModifiable(id);
        Set<Permission> permissions = resolvePermissions(request.permissionCodes());

        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);

        auditService.log("UPDATE_PERMISSIONS", "ROLE", role.getId(), null,
                Map.of("permissionCodes", request.permissionCodes()));
        return role.getPermissions().stream().map(Permission::getCode).toList();
    }

    /** Un rôle n'est modifiable QUE s'il appartient explicitement à l'entreprise courante (jamais un rôle système). */
    private Role findRoleModifiable(UUID id) {
        Role role = roleRepository.findByIdAccessibleForEntreprise(id, tenantContext.currentEntrepriseId())
                .orElseThrow(() -> BusinessException.notFound(ErrorCode.ROLE_NOT_FOUND, "Rôle introuvable"));
        if (role.isEstSysteme()) {
            throw BusinessException.unprocessable(ErrorCode.VALIDATION_ERROR,
                    "Les rôles système ne peuvent pas être modifiés directement — créez un rôle personnalisé "
                            + "(POST /api/v1/roles) si vous souhaitez un jeu de permissions différent");
        }
        return role;
    }

    private Set<Permission> resolvePermissions(List<String> codes) {
        List<Permission> all = permissionRepository.findAll();
        Set<Permission> resolved = all.stream().filter(p -> codes.contains(p.getCode())).collect(Collectors.toSet());
        if (resolved.size() != codes.size()) {
            throw BusinessException.badRequest(ErrorCode.VALIDATION_ERROR, "Un ou plusieurs codes de permission sont invalides");
        }
        return resolved;
    }

    private RoleResponse toResponse(Role r, UUID entrepriseId) {
        boolean modifiable = !r.isEstSysteme() && entrepriseId.equals(r.getEntrepriseId());
        return new RoleResponse(r.getId(), r.getCode(), r.getLibelle(), r.isEstSysteme(), modifiable);
    }
}
