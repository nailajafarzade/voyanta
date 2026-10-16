package com.voyanta.common.security;

import com.voyanta.common.config.VoyantaProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Service responsible for JWT handling: it creates access and refresh tokens for a user id,
 * validates them and reads the user id back out. Secret and token lifetimes come from
 * VoyantaProperties.
 *
 * Tokenin daxili detallarını (algoritm, secret) yalnız bu class bilir.
 * auth modulu bunu çağırır, amma necə işlədiyini bilmir.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final VoyantaProperties properties;

    public String generateAccessToken(UUID userId) {
        return generateToken(userId, properties.getJwt().getAccessTokenTtl().toMillis(), "access");
    }

    public String generateRefreshToken(UUID userId) {
        return generateToken(userId, properties.getJwt().getRefreshTokenTtl().toMillis(), "refresh");
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            return parseClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private String generateToken(UUID userId, long ttlMillis, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())   // <-- jti, hər çağırışda unikal
                .subject(userId.toString())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes());
    }
}