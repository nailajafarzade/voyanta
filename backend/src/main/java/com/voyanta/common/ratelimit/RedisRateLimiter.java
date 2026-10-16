package com.voyanta.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-based implementation of RateLimiter, registered as a Spring @Component.
 * It counts requests per key under a "rate:" prefix and lets the request through
 * while the counter stays inside the given limit and time window.
 *
 * Redis INCR atomikdir — paralel sorğularda belə race condition yaratmır.
 * Bucket4j kimi kitabxanalar burst/sliding-window ssenariləri üçün daha güclüdür,
 * amma bizim ehtiyac ("gündə N plan") üçün bu, sadə və kifayət qədər dəqiqdir.
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean tryConsume(String key, int limit, Duration window) {
        String redisKey = "rate:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, window);
        }

        return count != null && count <= limit;
    }
}