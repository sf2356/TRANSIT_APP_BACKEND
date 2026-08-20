package com.transit.platform.dossier.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeResponsableRequest(@NotNull UUID responsableId) {}
