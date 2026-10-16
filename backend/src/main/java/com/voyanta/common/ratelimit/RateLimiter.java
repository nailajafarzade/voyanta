package com.voyanta.common.ratelimit;

import java.time.Duration;

/**
 * Abstraction for counting requests per key inside a time window.
 * Services such as PlanGenerationService use it to enforce the daily plan limits
 * without depending on the concrete storage (currently Redis).
 */
public interface RateLimiter {
    /**
     * @return true — sorğuya icazə var (limit aşılmayıb), false — limit dolub
     */
    boolean tryConsume(String key, int limit, Duration window);
}