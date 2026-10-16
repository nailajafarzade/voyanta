package com.voyanta.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import com.voyanta.support.FakeGoogleTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends AbstractIntegrationTest {

    @Test
    void googleLoginIssuesTokensAndRefreshRotatesThem() throws Exception {
        String email = "oauth-" + System.nanoTime() + "@voyanta.test";
        JsonNode login = loginGoogle(email, "Test User");

        assertThat(login.path("accessToken").asText()).isNotBlank();
        assertThat(login.path("refreshToken").asText()).isNotBlank();
        assertThat(login.path("user").path("email").asText()).isEqualTo(email);
        assertThat(login.path("user").path("fullName").asText()).isEqualTo("Test User");

        String firstRefresh = login.path("refreshToken").asText();
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode refreshed = json(refreshResult).path("data");
        assertThat(refreshed.path("accessToken").asText()).isNotBlank();
        assertThat(refreshed.path("refreshToken").asText()).isNotEqualTo(firstRefresh);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void googleLoginReusesExistingUserByEmail() throws Exception {
        String email = "reuse-" + System.nanoTime() + "@voyanta.test";
        JsonNode first = loginGoogle(email, "First Name");
        JsonNode second = loginGoogle(email, "Second Name");

        assertThat(second.path("user").path("id").asText())
                .isEqualTo(first.path("user").path("id").asText());
        assertThat(second.path("user").path("fullName").asText()).isEqualTo("First Name");
    }

    @Test
    void invalidGoogleTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"not-a-real-google-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("INVALID_OAUTH_TOKEN"));
    }

    @Test
    void unknownProviderIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid:a@b.com:A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("UNKNOWN_PROVIDER"));
    }

    @Test
    void blankIdTokenFailsValidation() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void logoutRevokesRefreshTokens() throws Exception {
        String email = "logout-" + System.nanoTime() + "@voyanta.test";
        JsonNode login = loginGoogle(email, "Logout User");
        String accessToken = login.path("accessToken").asText();
        String refreshToken = login.path("refreshToken").asText();

        mockMvc.perform(withAuth(post("/api/auth/logout"), accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRefreshTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"totally-invalid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    // ------------------------------------------------------------------
    // Production incident-in reqres testi: AI provider (OpenAI) 429/5xx
    // verəndə Google girişi HƏLƏ DƏ UĞURLA işləməlidir.
    //
    // Giriş heç vaxt AI-a bağlı deyildi və indi də deyil; bu test yalnız
    // qarışıqlıq yaratmasın deyə qoyulub. Əvvəl plan generasiyası sorğu
    // thread-ində bloklanırdı, provider 429 verəndə bütün Tomcat worker
    // thread-ləri tutulurdu və giriş endpoint-i cavab verə bilmirdi.
    // ------------------------------------------------------------------

    @Test
    void googleLoginSucceedsWhileTheAiProviderIsFailing() throws Exception {
        fakeAiPlanGenerator.failNextCalls();

        String email = "ai-outage-" + System.nanoTime() + "@voyanta.test";
        JsonNode login = loginGoogle(email, "Test User");

        assertThat(login.path("accessToken").asText()).isNotBlank();
        assertThat(login.path("refreshToken").asText()).isNotBlank();
        assertThat(login.path("user").path("email").asText()).isEqualTo(email);
    }

    @Test
    void repeatedGoogleLoginsAllSucceedWhileTheAiProviderKeepsFailing() throws Exception {
        // A single 429 used to be enough to make the whole API unresponsive. Proving
        // repeated logins under a sustained AI outage shows the two paths are decoupled.
        fakeAiPlanGenerator.failNextCalls();

        for (int i = 0; i < 5; i++) {
            String email = "sustained-outage-" + i + "-" + System.nanoTime() + "@voyanta.test";
            JsonNode login = loginGoogle(email, "Test User " + i);

            assertThat(login.path("accessToken").asText())
                    .as("login #" + i + " must succeed even though the AI provider is down")
                    .isNotBlank();
        }
    }

    // ------------------------------------------------------------------
    // Provider outage vs. invalid token: the distinction the frontend needs.
    // A 503 is retryable, a 401 is not.
    // ------------------------------------------------------------------

    @Test
    void providerOutageReturnsServiceUnavailableNotUnauthorized() throws Exception {
        FakeGoogleTokenVerifier.failWithProviderUnavailable();

        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid:someone@voyanta.test:Someone\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("OAUTH_PROVIDER_UNAVAILABLE"));
    }

    @Test
    void providerOutageDoesNotLeakInternalDetail() throws Exception {
        FakeGoogleTokenVerifier.failWithProviderUnavailable();

        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid:someone@voyanta.test:Someone\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("Exception")
                        .doesNotContain("googleapis.com")
                        .doesNotContain("googleapis"));
    }

    @Test
    void genuinelyInvalidTokenStillReturnsUnauthorizedAfterTheFix() throws Exception {
        // The fix must not have loosened token validation.
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"forged-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("INVALID_OAUTH_TOKEN"));
    }

    @Test
    void providerRecoveryRestoresSuccessfulLogin() throws Exception {
        FakeGoogleTokenVerifier.failWithProviderUnavailable();
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid:outage@voyanta.test:Outage\"}"))
                .andExpect(status().isServiceUnavailable());

        FakeGoogleTokenVerifier.reset();

        String email = "recovered-" + System.nanoTime() + "@voyanta.test";
        JsonNode login = loginGoogle(email, "Recovered");
        assertThat(login.path("accessToken").asText()).isNotBlank();
    }
}
