package com.example.itfinance.security;

import com.example.itfinance.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {
    private static final long REFRESH_TOKEN_DAYS = 7L;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(Long userId, String username, String role, String realName) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getTokenExpireMinutes() * 60);
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of(
                        "userId", userId,
                        "role", role,
                        "realName", realName,
                        "tokenType", "access"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expireAt))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username, String role, String realName) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(REFRESH_TOKEN_DAYS * 24 * 60 * 60);
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of(
                        "userId", userId,
                        "role", role,
                        "realName", realName,
                        "tokenType", "refresh"))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expireAt))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey signingKey() {
        String raw = jwtProperties.getJwtSecret();
        if (raw == null || raw.isBlank() || raw.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret is missing or too short. Set APP_SECURITY_JWT_SECRET with at least 32 characters.");
        }
        return Keys.hmacShaKeyFor(raw.getBytes(StandardCharsets.UTF_8));
    }
}
