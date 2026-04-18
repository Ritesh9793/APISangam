package com.apimarketplace.security;

import com.apimarketplace.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public JwtService(
        @Value("${app.security.jwt.secret}") String secret,
        @Value("${app.security.jwt.access-token-minutes:60}") long accessTokenMinutes,
        @Value("${app.security.jwt.refresh-token-days:30}") long refreshTokenDays
    ) {
        this.signingKey = Keys.hmacShaKeyFor(normalizeSecret(secret));
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public String generateAccessToken(UserAccount user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("userId", user.getId().toString())
            .claim("role", user.getRole().name())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(Duration.ofMinutes(accessTokenMinutes))))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateToken(UserAccount user) {
        return generateAccessToken(user);
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private byte[] normalizeSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to prepare JWT secret", ex);
        }
    }
}
