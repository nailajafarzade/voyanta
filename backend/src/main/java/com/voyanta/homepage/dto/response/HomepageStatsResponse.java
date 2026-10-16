package com.voyanta.homepage.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO with the two numbers displayed on the homepage: how many travel plans
 * have been created and the average rating. It is also the object stored in the Redis
 * stats cache.
 */
public record HomepageStatsResponse(
        long plansCreated,
        BigDecimal avgRating
) {}