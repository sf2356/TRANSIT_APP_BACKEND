package com.transit.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inscription = création d'une nouvelle entreprise (tenant) avec son premier utilisateur
 * administrateur (rôle DIRECTEUR). Ce n'est pas l'ajout d'un utilisateur dans une entreprise
 * existante — pour cela voir POST /api/v1/utilisateurs, réservé aux utilisateurs déjà authentifiés.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 255) String nomEntreprise,
        @NotBlank @Email String emailEntreprise,
        @NotBlank @Size(max = 100) String nomAdmin,
        @NotBlank @Size(max = 100) String prenomAdmin,
        @NotBlank @Email String emailAdmin,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String motDePasse
) {}
