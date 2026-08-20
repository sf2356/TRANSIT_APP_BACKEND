package com.transit.platform.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateRoleRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z_]+$", message = "Le code doit être en MAJUSCULES_AVEC_UNDERSCORES") String code,
        @NotBlank @Size(max = 100) String libelle,
        List<String> permissionCodes
) {}
