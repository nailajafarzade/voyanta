package com.voyanta.common.ratelimit;

import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisRateLimiterIT extends AbstractIntegrationTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Test
    void incrementingInRedisEnforcesTheWindowLimit() {
        String key = "test:" + UUID.randomUUID();
        assertThat(rateLimiter.tryConsume(key, 2, Duration.ofMinutes(5))).isTrue();
        assertThat(rateLimiter.tryConsume(key, 2, Duration.ofMinutes(5))).isTrue();
        assertThat(rateLimiter.tryConsume(key, 2, Duration.ofMinutes(5))).isFalse();
    }
}
