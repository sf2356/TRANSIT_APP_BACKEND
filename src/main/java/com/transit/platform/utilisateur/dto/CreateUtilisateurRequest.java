package com.transit.platform.utilisateur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateUtilisateurRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email String email,
        String telephone,
        String villeAffectation,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String motDePasse,
        @NotEmpty(message = "Au moins un rôle doit être affecté") List<String> roleCodes
) {}
