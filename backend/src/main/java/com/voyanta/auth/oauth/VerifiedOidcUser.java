package com.voyanta.auth.oauth;

// Apple çox vaxt "name"-i YALNIZ ilk sign-in-də göndərir (sonrakı girişlərdə yox) —
// ona görə name nullable-dır, çağıran tərəf (OAuthService) buna fallback etməlidir.
/**
 * DTO holding the trusted claims taken from a verified Google or Apple ID token:
 * the provider's subject id, the email and the display name. OAuthService uses it to
 * find or create the matching Voyanta user.
 */
public record VerifiedOidcUser(
        String subject,
        String email,
        String name
) {}