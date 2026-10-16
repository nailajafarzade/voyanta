package com.voyanta.auth.dto.response;

import java.util.UUID;

/**
 * Small response DTO with the safe public fields of a user (id, full name, email).
 * It is nested inside AuthResponse so the client can show the logged-in user without
 * exposing the password hash or provider details.
 */
public record UserSummary(
        UUID id,
        String fullName,
        String email
) {}