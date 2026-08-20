package com.transit.platform.role;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.role.dto.PermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions", description = "Référentiel global des permissions disponibles")
public class PermissionController {

    private final RoleService roleService;

    public PermissionController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Lister toutes les permissions du référentiel (catalogue global, non filtré par entreprise)")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.of(roleService.listPermissions());
    }
}
