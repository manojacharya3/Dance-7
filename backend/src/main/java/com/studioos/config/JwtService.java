package com.studioos.config;

import com.studioos.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration accessTokenLifetime;

    public JwtService(
        @Value("${app.security.jwt-secret}") String secret,
        @Value("${app.security.access-token-minutes:15}") long accessTokenMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenLifetime = Duration.ofMinutes(accessTokenMinutes);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("uid", user.getId())
            .claim("tenantId", user.getTenantId())
            .claim("roles", user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTokenLifetime)))
            .signWith(signingKey)
            .compact();
    }

    public String subject(String token) {
        return claims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            claims(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
