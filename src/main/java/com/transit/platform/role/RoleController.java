package com.transit.platform.role;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.role.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Rôles", description = "Rôles système (partagés) et rôles personnalisés par entreprise")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Lister les rôles disponibles (système + personnalisés de l'entreprise)")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.of(roleService.listAvailable());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Operation(summary = "Créer un rôle personnalisé pour l'entreprise courante")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.of(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Operation(summary = "Renommer un rôle personnalisé (les rôles système sont refusés — 422)")
    public ApiResponse<RoleResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.of(roleService.update(id, request));
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "Lister les codes de permission d'un rôle")
    public ApiResponse<List<String>> getPermissions(@PathVariable UUID id) {
        return ApiResponse.of(roleService.getPermissionCodes(id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Operation(summary = "Remplacer le jeu de permissions d'un rôle personnalisé (jamais un rôle système — 422)")
    public ApiResponse<List<String>> updatePermissions(@PathVariable UUID id, @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ApiResponse.of(roleService.updatePermissions(id, request));
    }
}
