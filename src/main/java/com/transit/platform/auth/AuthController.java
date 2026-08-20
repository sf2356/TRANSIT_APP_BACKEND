package com.transit.platform.auth;

import com.transit.platform.auth.dto.*;
import com.transit.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, renouvellement de session")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une nouvelle entreprise (tenant) avec son premier utilisateur administrateur")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion — retourne un access token et un refresh token")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouveler l'access token à partir d'un refresh token valide")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (côté client : suppression des tokens ; aucune session serveur à invalider en JWT stateless)")
    public ApiResponse<Void> logout() {
        return ApiResponse.of(null);
    }

    @GetMapping("/me")
    @Operation(summary = "Profil, rôles et permissions de l'utilisateur authentifié")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.of(authService.me());
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Modifier son propre mot de passe (nécessite l'ancien mot de passe)")
    public com.transit.platform.common.ApiResponse<Void> changePassword(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.transit.platform.auth.dto.ChangePasswordRequest request) {
        authService.changePassword(request);
        return com.transit.platform.common.ApiResponse.of(null);
    }
}
