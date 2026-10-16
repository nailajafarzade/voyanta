package com.voyanta.auth.oauth;

import com.voyanta.common.config.VoyantaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Client component that verifies Google Sign-In ID tokens. It configures the shared
 * OidcTokenVerifier with Google's issuer and JWKS URL plus the configured client id,
 * and is only created when a Google client id is set in the configuration.
 */
@Component("google")
@Profile("!test")
@ConditionalOnExpression("'${voyanta.oauth.google.client-id:}'.trim().length() > 0")
class GoogleTokenVerifier implements ProviderTokenVerifier {

    private static final String ISSUER = "https://accounts.google.com";
    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    private final OidcTokenVerifier delegate;

    GoogleTokenVerifier(VoyantaProperties properties) {
        this.delegate = new OidcTokenVerifier(ISSUER, JWKS_URL, properties.getOauth().getGoogle().getClientId());
    }

    @Override
    public VerifiedOidcUser verify(String idToken) {
        return delegate.verify(idToken);
    }
}
