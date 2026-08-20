package com.transit.platform.auth.dto;

/** expiresIn en secondes, cohérent avec app.jwt.access-token-expiration-minutes. */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, MeResponse user) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds, MeResponse user) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
