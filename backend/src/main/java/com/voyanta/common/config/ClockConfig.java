package com.voyanta.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes the application {@link Clock} as a bean.
 *
 * <p>RecommendationService needs "now" to decide whether a wishlist row falls
 * inside the trending window (the last 30 days). Injecting a Clock instead of
 * calling {@code Instant.now()} keeps that boundary testable: a unit test can
 * fix the clock and assert the window deterministically instead of stubbing
 * around the real system time.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}