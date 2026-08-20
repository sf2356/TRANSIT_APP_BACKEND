package com.transit.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Émission et validation des JWT (access + refresh token).
 *
 * Le JWT ne contient QUE ce qui est nécessaire à l'autorisation (utilisateurId,
 * entrepriseId, email, permissions) — jamais de donnée sensible (mot de passe, etc.).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-token-expiration-minutes}") long accessTokenMinutes,
                       @Value("${app.jwt.refresh-token-expiration-days}") long refreshTokenDays) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public String generateAccessToken(UUID utilisateurId, UUID entrepriseId, String email, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("uid", utilisateurId.toString())
                .claim("eid", entrepriseId.toString())
                .claim("perms", permissions)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID utilisateurId, UUID entrepriseId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("uid", utilisateurId.toString())
                .claim("eid", entrepriseId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenDays, ChronoUnit.DAYS)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }

    /** Exposé pour que le login/refresh puisse renvoyer expiresIn au client (Prompt 04 §9). */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenMinutes * 60;
    }
}
