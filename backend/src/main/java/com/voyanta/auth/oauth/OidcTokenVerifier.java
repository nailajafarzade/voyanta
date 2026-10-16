package com.voyanta.auth.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import com.voyanta.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

/**
 * Shared helper that validates an OIDC ID token against a provider's issuer, JWKS keys
 * and client id, then returns the user claims as a VerifiedOidcUser.
 *
 * Google ve Apple her ikisi de OIDC-uygun oldugu icin ID token dogrulamasi ayni
 * mantikla is - farkli olan yalnizca issuer, JWKS URL'i ve client ID'dir.
 * GoogleTokenVerifier/AppleTokenVerifier tarafindan konfigurasyon olunup kullanilir.
 *
 * KRITIK KONU - iki tur hata birbirinden ayrilir:
 *
 * 1) Tokenin KENDISI ile ilgili hata (parse, imza, iss, aud, exp)
 *    -> 401 INVALID_OAUTH_TOKEN. "Kullanici kotu token gonderdi" demektir, 401 dogru.
 *
 * 2) PROVIDER'in anahtar endpoint'ine cikisin basarisiz olmasi
 *    (IOException: DNS, baglanti kurulamamasi, socket timeout, JWKS 5xx/429)
 *    -> 503 OAUTH_PROVIDER_UNAVAILABLE.
 *    Bu kullaniciya ait bir hata DEGILDIR; giris "dogrulanmadi" degil, sadece
 *    provider su an hazir degil. Bunu 401 olarak dondurmak frontend'e yalan
 *    soyluyordu ("Google/Apple ile giris tespitlenmedi") ve kullaniciyi kendi
 *    hatasina inandirip sonsuz denemesine yol aciyordu.
 *
 * 3) JWKS ZAMAN ASIMI: Nimbus'un varsayilan JWKS connect/read sureleri yalnizca
 *    500 ms'dir. Uzak bir VPS'te googleapis.com'a gidis 500 ms'yi asdiginda ag
 *    hatasi IOException olarak yukselir ve (eski kodda) o da yine 401'e
 *    donusurdu. Asagida gercekci timeout'lar ve otomatik tekrar deneme var.
 *
 * Neden IOException ayrimi bu kadar onemli: bu, production'daki "Google login
 * calismiyor" belirtilerinin buyuk bolumunu tek basina yapan seydir. OpenAI
 * 429'unda oldugu kimi, xarici servisin anlik durumu kullanici token'inin
 * gecerliligine dair karar veremez.
 *
 * Istisna zinciri (IOException) tum istisna yigini boyunca aranir, cunku Nimbus
 * JWKS hatasini bazen bir baz bazen de JOSEException icinde sunar. Sebep
 * hicbir zaman loglanmirdi; artik hem loglanir hem de dogru HTTP statusuna
 * cevrilir.
 */
@Slf4j
class OidcTokenVerifier {

    private static final int JWKS_CONNECT_TIMEOUT_MS = 3_000;
    private static final int JWKS_READ_TIMEOUT_MS = 5_000;

    private final String issuerUrl;
    private final IDTokenValidator validator;

    OidcTokenVerifier(String issuerUrl, String jwksUrl, String clientId) {
        this.issuerUrl = issuerUrl;
        try {
            URL jwks = new URL(jwksUrl);
            DefaultResourceRetriever retriever = new DefaultResourceRetriever(
                    JWKS_CONNECT_TIMEOUT_MS, JWKS_READ_TIMEOUT_MS, JWKSourceBuilder.DEFAULT_HTTP_SIZE_LIMIT);

            // JWKSourceBuilder kullanilir (dogrudan RemoteJWKSet constructoru
            // deprecated'dur) ve retrying(true) gecici ag hatalarini otomatik tekrar
            // dener. Anahtar seti onbellege alinir; her giriste Google'a istek atilmaz.
            JWKSource<SecurityContext> jwkSource = JWKSourceBuilder.<SecurityContext>create(jwks, retriever)
                    .retrying(true)
                    .build();

            this.validator = new IDTokenValidator(
                    new Issuer(issuerUrl),
                    new ClientID(clientId),
                    new JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource),
                    null
            );
        } catch (MalformedURLException e) {
            throw new IllegalStateException("JWKS URL yanlışdır: " + jwksUrl, e);
        }
    }

    VerifiedOidcUser verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw invalidToken();
        }

        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(idToken);
        } catch (ParseException e) {
            // Genel olarak JWT bile degil - bu, gercekten gecersiz token durumudur.
            log.warn("[{}] ID token parse olunmadı: {}", issuerUrl, e.getMessage());
            throw invalidToken();
        }

        IDTokenClaimsSet claims;
        try {
            // nonce dogrulanmaz: bu sunucu tarafi dogrulama akisidir (frontend provider
            // ile gorusup id_token'i bize verir), client-side replay riski farkli kat.
            claims = validator.validate(jwt, null);
        } catch (Exception e) {
            if (causedByIOException(e)) {
                // Provider'in acik anahtar endpoint'ine cikis basarisiz oldu. Bu bir
                // kimlik dogrulama hatasi DEGIL - yeniden denemekle duzelir.
                log.error("[{}] JWKS endpoint'i ulaşılamadı, ID token doğrulanamadı: {}",
                        issuerUrl, e.getMessage(), e);
                throw providerUnavailable();
            }

            // Imza/issuer/audience/exp uymuyor - token gercekten gecersizdir.
            log.warn("[{}] ID token doğrulanmadı: {}", issuerUrl, e.getMessage());
            throw invalidToken();
        }

        String email = claims.getStringClaim("email");
        if (email == null || email.isBlank()) {
            log.warn("[{}] ID token içinde email claim'i yok", issuerUrl);
            throw invalidToken();
        }

        String name = claims.getStringClaim("name");
        return new VerifiedOidcUser(claims.getSubject().getValue(), email, name);
    }

    private static boolean causedByIOException(Throwable error) {
        Throwable current = error;
        // Maksimum derinlik sinir: sonsuz dongu riskine karsi.
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "OAuth token doğrulanmadı");
    }

    private static ApiException providerUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OAUTH_PROVIDER_UNAVAILABLE",
                "Giriş xidməti hazır deyil, bir az sonra yenidən yoxla");
    }
}
