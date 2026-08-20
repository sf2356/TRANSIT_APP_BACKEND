package com.transit.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire") String ancienMotDePasse,
        @NotBlank @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères") String nouveauMotDePasse
) {}