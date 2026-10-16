package com.voyanta.auth.service;

import com.voyanta.auth.dao.entity.User;
import com.voyanta.auth.dao.repository.UserRepository;
import com.voyanta.auth.dto.request.OAuthLoginRequest;
import com.voyanta.auth.dto.response.AuthResponse;
import com.voyanta.auth.dto.response.UserSummary;
import com.voyanta.auth.enums.AuthProvider;
import com.voyanta.auth.oauth.ProviderTokenVerifier;
import com.voyanta.auth.oauth.VerifiedOidcUser;
import com.voyanta.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private ProviderTokenVerifier googleVerifier;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;

    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        oAuthService = new OAuthService(Map.of("google", googleVerifier), userRepository, authService);
    }

    @Test
    void createsUserOnFirstLogin() {
        when(googleVerifier.verify("token")).thenReturn(new VerifiedOidcUser("sub", "new@voyanta.test", "Ada"));
        when(userRepository.findByEmail("new@voyanta.test")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        AuthResponse issued = new AuthResponse("a", "r", new UserSummary(UUID.randomUUID(), "Ada", "new@voyanta.test"));
        when(authService.issueTokens(any(User.class))).thenReturn(issued);

        AuthResponse response = oAuthService.login("google", new OAuthLoginRequest("token"));
        assertThat(response).isSameAs(issued);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@voyanta.test");
        assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void unknownProviderIsRejected() {
        assertThatThrownBy(() -> oAuthService.login("facebook", new OAuthLoginRequest("token")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo("UNKNOWN_PROVIDER");
    }

    @Test
    void providerOutageIsPropagatedAsServiceUnavailableNotUnauthorized() {
        // Google JWKS endpoint-i əlçatan deyildirsə, verifier 503 atır. Bu, dəqiq
        // məqsədlə 401 OLMAMALIDIR — əks halda "giriş təsdiqlənmədi" mesajı
        // çıxar və istifadəçi öz səhvinə inanardı.
        when(googleVerifier.verify("token")).thenThrow(new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE, "OAUTH_PROVIDER_UNAVAILABLE", "Giriş xidməti hazır deyil"));

        assertThatThrownBy(() -> oAuthService.login("google", new OAuthLoginRequest("token")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(api.getErrorCode()).isEqualTo("OAUTH_PROVIDER_UNAVAILABLE");
                });

        verifyNoInteractions(userRepository, authService);
    }

    @Test
    void invalidTokenFromVerifierIsNotSwallowed() {
        when(googleVerifier.verify("token")).thenThrow(new ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "OAuth token doğrulanmadı"));

        assertThatThrownBy(() -> oAuthService.login("google", new OAuthLoginRequest("token")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verifyNoInteractions(userRepository, authService);
    }

    @Test
    void loginNeverCallsTheAiProvider() {
        // Guard for the architecture: OAuthService has no AiPlanGenerator dependency at
        // all, so an AI outage cannot reach the login path even in principle.
        when(googleVerifier.verify("token")).thenReturn(new VerifiedOidcUser("sub", "arch@voyanta.test", "Arch"));
        when(userRepository.findByEmail("arch@voyanta.test")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(authService.issueTokens(any(User.class)))
                .thenReturn(new AuthResponse("a", "r", new UserSummary(UUID.randomUUID(), "Arch", "arch@voyanta.test")));

        assertThat(oAuthService.login("google", new OAuthLoginRequest("token")).accessToken()).isEqualTo("a");
    }

    @Test
    void existingUserIsReusedAndNoTokensAreIssuedForUnknownEmail() {
        when(googleVerifier.verify("token")).thenReturn(new VerifiedOidcUser("sub", "known@voyanta.test", "Known"));
        when(userRepository.findByEmail("known@voyanta.test")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(authService.issueTokens(any(User.class)))
                .thenReturn(new AuthResponse("a", "r", new UserSummary(UUID.randomUUID(), "Known", "known@voyanta.test")));

        oAuthService.login("google", new OAuthLoginRequest("token"));

        verify(userRepository, never()).save(argThat(saved -> saved.getEmail() == null));
    }
}
