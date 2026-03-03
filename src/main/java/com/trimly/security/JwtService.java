package com.trimly.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Handles short-lived JWT access tokens (default 24h).
 * Refresh tokens are opaque UUIDs stored in the refresh_tokens table —
 * they are not JWTs and never expire server-side until logout or 30 days.
 */
@Service @Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-expiration-ms:31536000000}")
    private long accessExpirationMs;

    public String generateAccessToken(UserDetails user) {
        return Jwts.builder()
            .subject(user.getUsername())           // phone for customers, email for barbers/admin
            .claim("role", user.getAuthorities().iterator().next().getAuthority())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
            .signWith(getKey())
            .compact();
    }

    public boolean isValid(String token, UserDetails user) {
        try {
            return extractUsername(token).equals(user.getUsername()) && !isExpired(token);
        } catch (Exception e) {
            log.debug("JWT invalid: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return claim(token, Claims::getSubject);
    }

    public boolean isExpired(String token) {
        try {
            return claim(token, Claims::getExpiration).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private <T> T claim(String token, Function<Claims, T> fn) {
        return fn.apply(Jwts.parser()
            .verifyWith(getKey()).build()
            .parseSignedClaims(token).getPayload());
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public long getAccessExpirationMs() { return accessExpirationMs; }
}

