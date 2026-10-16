package com.voyanta.auth.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.voyanta.common.exception.ApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OidcTokenVerifier, focused on the error CLASSIFICATION that caused
 * the production incident.
 *
 * These are the regression tests for the bug: before the fix, any exception thrown
 * while validating a token (including "cannot reach the provider's JWKS endpoint")
 * was converted into 401 INVALID_OAUTH_TOKEN, so a temporary Google-side or network
 * problem looked to the user exactly like "your Google login was not confirmed".
 *
 * Nothing here touches real Google. A local MockWebServer plays the role of the
 * JWKS endpoint and an in-memory RSA key pair plays Google's signing key, so the
 * tests need no network access and no real credentials.
 */
class OidcTokenVerifierTest {

    private static final String ISSUER = "https://accounts.google.com";
    private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
    private static final String SUBJECT = "google-subject-123";

    private MockWebServer jwksServer;
    private RSAKey signingKey;
    private OidcTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        jwksServer = new MockWebServer();
        jwksServer.start();

        signingKey = new RSAKeyGenerator(2048).keyID("test-key-1").generate();
        jwksServer.enqueue(jwksResponse());

        verifier = new OidcTokenVerifier(ISSUER, jwksServer.url("/oauth2/v3/certs").toString(), CLIENT_ID);
    }

    @AfterEach
    void tearDown() throws IOException {
        jwksServer.shutdown();
    }

    // ------------------------------------------------------------------
    // 1) Valid tokens
    // ------------------------------------------------------------------

    @Test
    void validGoogleIdTokenIsAccepted() throws Exception {
        VerifiedOidcUser user = verifier.verify(signToken(CLIENT_ID).serialize());

        assertThat(user.subject()).isEqualTo(SUBJECT);
        assertThat(user.email()).isEqualTo("ada@voyanta.test");
        assertThat(user.name()).isEqualTo("Ada");
    }

    @Test
    void nullNameClaimIsTolerated() throws Exception {
        // Apple yalnizca ilk sign-in-de "name" gonderir; null olmamalidir.
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                baseClaims().audience(CLIENT_ID).claim("email", "ada@voyanta.test").build());
        jwt.sign(new RSASSASigner(signingKey.toRSAPrivateKey()));

        VerifiedOidcUser user = verifier.verify(jwt.serialize());

        assertThat(user.email()).isEqualTo("ada@voyanta.test");
        assertThat(user.name()).isNull();
    }

    // ------------------------------------------------------------------
    // 2) Genuinely invalid tokens -> 401 INVALID_OAUTH_TOKEN.
    //    These must NOT be loosened: the fix targeted infrastructure failures,
    //    not acceptance of bad tokens.
    // ------------------------------------------------------------------

    @Test
    void garbageTokenIsUnauthorized() {
        assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void blankTokenIsUnauthorized() {
        assertThatThrownBy(() -> verifier.verify("   "))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void nullTokenIsUnauthorized() {
        assertThatThrownBy(() -> verifier.verify(null))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void tokenSignedByAnotherKeyIsUnauthorized() throws Exception {
        // JWKS endpoint basqa acari tanyir -> imza uygun gelmir.
        jwksServer.enqueue(jwksResponse());
        RSAKey foreignKey = new RSAKeyGenerator(2048).keyID("test-key-1").generate();

        assertThatThrownBy(() -> verifier.verify(signTokenWith(foreignKey, CLIENT_ID).serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void tokenSignedWithUnknownKeyIdIsUnauthorized() throws Exception {
        // JWKS-dekinden ferqli "kid" -> acar tapilmir.
        jwksServer.enqueue(jwksResponse());
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("some-other-kid").build(),
                baseClaims().audience(CLIENT_ID).claim("email", "ada@voyanta.test").build());
        jwt.sign(new RSASSASigner(signingKey.toRSAPrivateKey()));

        assertThatThrownBy(() -> verifier.verify(jwt.serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void tokenWithWrongIssuerIsUnauthorized() throws Exception {
        // aud duzgundur, iss bashqadir: bashqa provider token-i qebul edilmemelidir.
        jwksServer.enqueue(jwksResponse());
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                baseClaims()
                        .issuer("https://appleid.apple.com")
                        .audience(CLIENT_ID)
                        .claim("email", "ada@voyanta.test")
                        .build());
        jwt.sign(new RSASSASigner(signingKey.toRSAPrivateKey()));

        assertThatThrownBy(() -> verifier.verify(jwt.serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void tokenForAnotherClientIdIsUnauthorized() throws Exception {
        // Bu, "Google Client ID duzgun islədilir" testidir: aud claim-i basqa
        // client id-dir, ona gore token bu tetbiqe aid deyil.
        jwksServer.enqueue(jwksResponse());
        SignedJWT jwt = signTokenWith(signingKey, "some-other-client.apps.googleusercontent.com");

        assertThatThrownBy(() -> verifier.verify(jwt.serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        jwksServer.enqueue(jwksResponse());
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                baseClaims()
                        .audience(CLIENT_ID)
                        .claim("email", "ada@voyanta.test")
                        .expirationTime(new Date(System.currentTimeMillis() - 60_000))
                        .build());
        jwt.sign(new RSASSASigner(signingKey.toRSAPrivateKey()));

        assertThatThrownBy(() -> verifier.verify(jwt.serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    @Test
    void tokenWithoutEmailClaimIsUnauthorized() throws Exception {
        jwksServer.enqueue(jwksResponse());
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                baseClaims().audience(CLIENT_ID).build());
        jwt.sign(new RSASSASigner(signingKey.toRSAPrivateKey()));

        assertThatThrownBy(() -> verifier.verify(jwt.serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertInvalidToken);
    }

    // ------------------------------------------------------------------
    // 3) Provider / network failures -> 503 OAUTH_PROVIDER_UNAVAILABLE.
    //    THE CORE OF THE PRODUCTION BUG. Each of these previously returned 401,
    //    telling the user their Google login failed when Google was merely
    //    unreachable.
    // ------------------------------------------------------------------

    @Test
    void unreachableJwksEndpointIsServiceUnavailableNotUnauthorized() throws Exception {
        // Bagli port -> connection refused -> IOException.
        OidcTokenVerifier offlineVerifier = new OidcTokenVerifier(
                ISSUER, "http://127.0.0.1:1/oauth2/v3/certs", CLIENT_ID);

        assertThatThrownBy(() -> offlineVerifier.verify(signToken(CLIENT_ID).serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(this::assertProviderUnavailable);
    }

    @Test
    void jwksConnectionTimeoutIsServiceUnavailableNotUnauthorized() throws Exception {
        // Server qebul edir amma hec vaxt cavab yazmir -> socket read timeout.
        // Bu, uzaq VPS-de Google-a gedis yavasladiqda en cox rast gelinen haldir
        // (Nimbus-un default timeout-lari comi 500 ms idi).
        try (MockWebServer silentProvider = new MockWebServer()) {
            silentProvider.start();
            silentProvider.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

            OidcTokenVerifier slowVerifier = new OidcTokenVerifier(
                    ISSUER, silentProvider.url("/oauth2/v3/certs").toString(), CLIENT_ID);

            assertThatThrownBy(() -> slowVerifier.verify(signToken(CLIENT_ID).serialize()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(this::assertProviderUnavailable);
        }
    }

    @Test
    void jwksEndpointReturningServerErrorIsServiceUnavailableNotUnauthorized() throws Exception {
        try (MockWebServer brokenProvider = new MockWebServer()) {
            brokenProvider.start();
            brokenProvider.enqueue(new MockResponse().setResponseCode(500).setBody("upstream boom"));

            OidcTokenVerifier failingVerifier = new OidcTokenVerifier(
                    ISSUER, brokenProvider.url("/oauth2/v3/certs").toString(), CLIENT_ID);

            assertThatThrownBy(() -> failingVerifier.verify(signToken(CLIENT_ID).serialize()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(this::assertProviderUnavailable);
        }
    }

    @Test
    void jwksEndpointReturningRateLimitIsServiceUnavailableNotUnauthorized() throws Exception {
        // Google ozu 429 vera biler - bu da istifadecinin gunahi deyil.
        try (MockWebServer throttledProvider = new MockWebServer()) {
            throttledProvider.start();
            throttledProvider.enqueue(new MockResponse().setResponseCode(429).setBody("slow down"));

            OidcTokenVerifier throttledVerifier = new OidcTokenVerifier(
                    ISSUER, throttledProvider.url("/oauth2/v3/certs").toString(), CLIENT_ID);

            assertThatThrownBy(() -> throttledVerifier.verify(signToken(CLIENT_ID).serialize()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(this::assertProviderUnavailable);
        }
    }

    @Test
    void unparseableJwksBodyIsServiceUnavailableNotUnauthorized() throws Exception {
        // Google 200 qaytarir amma govdesi JSON deyil -> techizetci xetasi,
        // istifadecinin token-i deyil.
        try (MockWebServer garbageProvider = new MockWebServer()) {
            garbageProvider.start();
            garbageProvider.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("<html>not json at all</html>"));

            OidcTokenVerifier garbageVerifier = new OidcTokenVerifier(
                    ISSUER, garbageProvider.url("/oauth2/v3/certs").toString(), CLIENT_ID);

            assertThatThrownBy(() -> garbageVerifier.verify(signToken(CLIENT_ID).serialize()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(this::assertProviderUnavailable);
        }
    }

    // ------------------------------------------------------------------
    // 4) Resilience: transient failure then success.
    //    retrying(true) is meant to absorb short blips, so a first-attempt
    //    failure followed by a good response must still sign the user in.
    //    This is the "worked previously, broke today" case.
    // ------------------------------------------------------------------

    @Test
    void transientJwksFailureFollowedBySuccessStillAuthenticates() throws Exception {
        try (MockWebServer flakyProvider = new MockWebServer()) {
            flakyProvider.start();
            // Ilk cehd ugursuz, ikinci cehd saglam cavab qaytarir.
            flakyProvider.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
            flakyProvider.enqueue(jwksResponse());

            OidcTokenVerifier flakyVerifier = new OidcTokenVerifier(
                    ISSUER, flakyProvider.url("/oauth2/v3/certs").toString(), CLIENT_ID);

            VerifiedOidcUser user = flakyVerifier.verify(signToken(CLIENT_ID).serialize());

            assertThat(user.email()).isEqualTo("ada@voyanta.test");
            assertThat(flakyProvider.getRequestCount())
                    .as("a transient JWKS failure must be retried, not surfaced to the user")
                    .isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void jwksKeySetIsCachedAcrossVerifications() throws Exception {
        // Ilk dogrulama JWKS-i cekir, ikincisi eyni acar destinden istifade edir.
        verifier.verify(signToken(CLIENT_ID).serialize());
        long afterFirst = jwksServer.getRequestCount();

        verifier.verify(signToken(CLIENT_ID).serialize());

        assertThat(jwksServer.getRequestCount())
                .as("JWK set is cached, so a second sign-in must not re-fetch the keys")
                .isEqualTo(afterFirst);
    }

    // ------------------------------------------------------------------
    // 5) Cause visibility and no leakage.
    //    The original handler discarded the exception, which is why the real
    //    cause could never be diagnosed in production.
    // ------------------------------------------------------------------

    @Test
    void providerFailureDoesNotLeakInternalDetailToTheClient() {
        OidcTokenVerifier offlineVerifier = new OidcTokenVerifier(
                ISSUER, "http://127.0.0.1:1/oauth2/v3/certs", CLIENT_ID);

        assertThatThrownBy(() -> offlineVerifier.verify(signToken(CLIENT_ID).serialize()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("OAUTH_PROVIDER_UNAVAILABLE");
                    assertThat(api.getMessage())
                            .as("client-facing message must not leak internal detail")
                            .doesNotContain("127.0.0.1")
                            .doesNotContain("Exception")
                            .doesNotContain("connect");
                });
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void assertInvalidToken(Throwable ex) {
        ApiException api = (ApiException) ex;
        assertThat(api.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.getErrorCode()).isEqualTo("INVALID_OAUTH_TOKEN");
    }

    private void assertProviderUnavailable(Throwable ex) {
        ApiException api = (ApiException) ex;
        assertThat(api.getStatus())
                .as("a provider/network failure must NEVER be reported as 401")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(api.getErrorCode()).isEqualTo("OAUTH_PROVIDER_UNAVAILABLE");
    }

    private JWTClaimsSet.Builder baseClaims() {
        long now = System.currentTimeMillis();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .issueTime(new Date(now))
                .expirationTime(new Date(now + 300_000));
    }

    private SignedJWT signToken(String audience) throws Exception {
        return signTokenWith(signingKey, audience);
    }

    private SignedJWT signTokenWith(RSAKey key, String audience) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                baseClaims()
                        .audience(audience)
                        .claim("email", "ada@voyanta.test")
                        .claim("name", "Ada")
                        .build());
        jwt.sign(new RSASSASigner(key.toRSAPrivateKey()));
        return jwt;
    }

    private MockResponse jwksResponse() {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(new JWKSet(signingKey.toPublicJWK()).toString());
    }
}
