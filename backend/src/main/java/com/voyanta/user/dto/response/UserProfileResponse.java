package com.voyanta.user.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO with the profile fields shown on the user's own account page: id, full
 * name, email, phone and the registration date. The password hash and sign-in provider
 * are left out on purpose.
 */
public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Instant createdAt
) {}