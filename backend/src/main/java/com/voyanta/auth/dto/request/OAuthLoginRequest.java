package com.voyanta.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the POST /api/auth/oauth/{provider} endpoint. It carries the ID token
 * that the frontend already received from Google or Apple, so the backend can verify it
 * and sign the user in.
 */
public record OAuthLoginRequest(
        @NotBlank
        String idToken
) {}