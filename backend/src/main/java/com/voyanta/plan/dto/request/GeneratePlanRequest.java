package com.voyanta.plan.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the POST /api/plans/generate endpoint. It carries only the survey
 * session id, because all the travel preferences are already stored in that session.
 */
public record GeneratePlanRequest(
        @NotBlank
        String surveySessionId
) {}