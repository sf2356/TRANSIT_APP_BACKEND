package com.transit.platform.role.dto;

import java.util.UUID;

public record RoleResponse(UUID id, String code, String libelle, boolean estSysteme, boolean modifiable) {}
