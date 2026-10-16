package com.voyanta.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the POST /api/auth/refresh endpoint. It carries the refresh token
 * that the client wants to exchange for a new pair of access and refresh tokens.
 */
public record RefreshRequest(
        @NotBlank
        String refreshToken
) {}