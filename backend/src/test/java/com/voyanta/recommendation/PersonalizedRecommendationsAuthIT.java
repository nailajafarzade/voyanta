package com.voyanta.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The homepage section "Sənin üçün seçilmiş yerlər" (Picked for you) is personalized
 * data, so a guest must never receive it. The endpoint is protected by the existing JWT
 * filter chain and @PreAuthorize - these tests lock that behaviour in.
 */
class PersonalizedRecommendationsAuthIT extends AbstractIntegrationTest {

    @Test
    void guestsDoNotReceivePersonalizedDestinations() throws Exception {
        mockMvc.perform(get("/api/recommendations/personalized"))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    JsonNode body = json(result);
                    assertThat(body.path("success").asBoolean()).isFalse();
                    assertThat(body.path("errorCode").asText()).isNotBlank();
                    assertThat(body.path("data").isMissingNode() || body.path("data").isNull())
                            .as("no destination list may be returned to a guest")
                            .isTrue();
                });
    }

    @Test
    void anInvalidTokenIsAlsoRejected() throws Exception {
        mockMvc.perform(get("/api/recommendations/personalized")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAuthenticatedUserStillReceivesThePersonalizedList() throws Exception {
        JsonNode auth = loginGoogle("picked-" + System.nanoTime() + "@voyanta.test", "Traveller");

        JsonNode recommended = json(mockMvc.perform(
                        withAuth(get("/api/recommendations/personalized"), auth.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andReturn()).path("data");

        assertThat(recommended.isArray()).isTrue();
        assertThat(recommended).isNotEmpty();
    }

    @Test
    void personalizedDestinationsCarryAnImageUrlField() throws Exception {
        JsonNode auth = loginGoogle("picked-img-" + System.nanoTime() + "@voyanta.test", "Traveller");

        JsonNode recommended = json(mockMvc.perform(
                        withAuth(get("/api/recommendations/personalized"), auth.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andReturn()).path("data");

        // The test profile has no Unsplash key, so the service returns null and the
        // database value is used as the fallback. Either way the field must exist and
        // be a string - the frontend always renders the same shape.
        assertThat(recommended.get(0).has("imageUrl")).isTrue();
        assertThat(recommended.get(0).path("imageUrl").isTextual()).isTrue();
    }
}
