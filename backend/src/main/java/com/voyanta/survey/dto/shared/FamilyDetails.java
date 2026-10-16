package com.voyanta.survey.dto.shared;

// Yalnız companion = FAMILY seçiləndə doldurulur
/**
 * Shared DTO with the number of adults and children travelling together. It is only
 * filled in for family trips and is sent to the AI so the plan fits the group.
 */
public record FamilyDetails(
        int adults,
        int children
) {}