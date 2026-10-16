//package com.voyanta.auth.oauth;
//
//import com.voyanta.common.config.VoyantaProperties;
//import org.springframework.stereotype.Component;
//
//// MÜVƏQQƏTİ SÖNDÜRÜLÜB — qeydiyyat hazırda yalnız Google ilədir. Geri açmaq üçün
//// aşağıdakı @Component sətrini aç — OAuthService-də HEÇ NƏYİ dəyişməyə ehtiyac yoxdur,
//// provider dispatch Map<String, ProviderTokenVerifier> üzərindən avtomatik işləyir.
//// @Component("apple")
//class AppleTokenVerifier implements ProviderTokenVerifier {
//
//    private static final String ISSUER = "https://appleid.apple.com";
//    private static final String JWKS_URL = "https://appleid.apple.com/auth/keys";
//
//    private final OidcTokenVerifier delegate;
//
//    AppleTokenVerifier(VoyantaProperties properties) {
//        this.delegate = new OidcTokenVerifier(ISSUER, JWKS_URL, properties.getOauth().getApple().getClientId());
//    }
//
//    @Override
//    public VerifiedOidcUser verify(String idToken) {
//        return delegate.verify(idToken);
//    }
//}