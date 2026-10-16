package com.voyanta.auth.dto.response;

/**
 * Response DTO returned by every successful auth operation (register, login, OAuth login
 * and refresh). It gives the client a new access token, a refresh token and a short
 * summary of the signed-in user.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {}