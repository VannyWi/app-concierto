package com.example.demo.security;

import com.example.demo.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String SESSION_ID_CLAIM = "sessionId";

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") Duration expiration) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must contain at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    public String createToken(UserEntity user, UUID sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().getRoleName())
                .claim(SESSION_ID_CLAIM, sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public Optional<JwtPayload> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String sessionId = claims.get(SESSION_ID_CLAIM, String.class);
            if (claims.getSubject() == null || sessionId == null) {
                return Optional.empty();
            }
            return Optional.of(new JwtPayload(claims.getSubject(), UUID.fromString(sessionId)));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record JwtPayload(String username, UUID sessionId) {
    }
}
