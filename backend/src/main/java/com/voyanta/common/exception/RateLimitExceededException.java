package com.voyanta.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user reaches the allowed number of plan generations,
 * for example the daily limit for anonymous or authenticated users.
 * Returns HTTP 429 with the error code "RATE_LIMIT_EXCEEDED".
 */
public class RateLimitExceededException extends ApiException {
    public RateLimitExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", message);
    }
}