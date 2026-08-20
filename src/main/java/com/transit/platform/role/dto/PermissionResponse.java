package com.transit.platform.role.dto;

import java.util.UUID;

public record PermissionResponse(UUID id, String code, String module, String description) {}
