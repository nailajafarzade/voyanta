package com.voyanta.auth.oauth;

/**
 * Interface for verifying a social provider's ID token. Each provider has its own
 * implementation registered under a bean name ("google", "apple"), which lets OAuthService
 * choose the right one from the URL without provider-specific if/else code.
 */
public interface ProviderTokenVerifier {
    VerifiedOidcUser verify(String idToken);
}