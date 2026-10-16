package com.voyanta.common.security;

import com.voyanta.common.config.VoyantaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        VoyantaProperties properties = new VoyantaProperties();
        properties.getJwt().setSecret("test-jwt-secret-must-be-at-least-32-bytes-long");
        properties.getJwt().setAccessTokenTtl(Duration.ofMinutes(15));
        properties.getJwt().setRefreshTokenTtl(Duration.ofDays(30));
        jwtService = new JwtService(properties);
    }

    @Test
    void accessTokenRoundTrip() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void malformedTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        VoyantaProperties properties = new VoyantaProperties();
        properties.getJwt().setSecret("test-jwt-secret-must-be-at-least-32-bytes-long");
        properties.getJwt().setAccessTokenTtl(Duration.ofMillis(1));
        properties.getJwt().setRefreshTokenTtl(Duration.ofDays(1));
        JwtService shortLived = new JwtService(properties);
        String token = shortLived.generateAccessToken(UUID.randomUUID());
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(shortLived.isTokenValid(token)).isFalse();
    }
}
