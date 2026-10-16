package com.voyanta.support;

import com.voyanta.auth.oauth.ProviderTokenVerifier;
import com.voyanta.auth.oauth.VerifiedOidcUser;
import com.voyanta.common.exception.ApiException;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Google OIDC verifier used only in tests. Tokens of the form {@code valid:email:name}
 * are accepted; anything else is rejected the same way a bad Google ID token would be.
 *
 * It can also simulate a PROVIDER OUTAGE, which is the condition that produced the
 * production incident. That is the important case to cover end to end: when Google is
 * unreachable the endpoint must answer 503, never 401. The real
 * {@code OidcTokenVerifier} is unit-tested separately against a MockWebServer, so this
 * stub only needs to reproduce the resulting ApiException faithfully.
 */
@Component("google")
@Primary
public class FakeGoogleTokenVerifier implements ProviderTokenVerifier {

    private static final AtomicReference<ApiException> FAILURE = new AtomicReference<>();

    @Override
    public VerifiedOidcUser verify(String idToken) {
        ApiException forced = FAILURE.get();
        if (forced != null) {
            throw forced;
        }

        if (idToken != null && idToken.startsWith("valid:")) {
            String[] parts = idToken.split(":", 3);
            String email = parts.length > 1 ? parts[1] : "user@example.com";
            String name = parts.length > 2 ? parts[2] : email;
            return new VerifiedOidcUser("google-sub-" + email, email, name);
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "OAuth token doğrulanmadı");
    }

    /** Simulates Google (or the network to Google) being unavailable. */
    public static void failWithProviderUnavailable() {
        FAILURE.set(new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE, "OAUTH_PROVIDER_UNAVAILABLE",
                "Giriş xidməti hazır deyil, bir az sonra yenidən yoxla"));
    }

    /** Simulates a genuinely broken token. */
    public static void failWithInvalidToken() {
        FAILURE.set(new ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "OAuth token doğrulanmadı"));
    }

    public static void reset() {
        FAILURE.set(null);
    }
}
