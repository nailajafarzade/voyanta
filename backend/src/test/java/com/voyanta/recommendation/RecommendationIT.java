package com.voyanta.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationIT extends AbstractIntegrationTest {

    @Test
    void personalizedRecommendationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/recommendations/personalized"))
                .andExpect(status().isForbidden());
    }

    @Test
    void newUserGetsFeaturedDestinations() throws Exception {
        JsonNode auth = loginGoogle("reco-new-" + System.nanoTime() + "@voyanta.test", "New");
        JsonNode featured = json(mockMvc.perform(get("/api/destinations/featured"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        JsonNode recommended = json(mockMvc.perform(
                        withAuth(get("/api/recommendations/personalized").param("limit", "4"), auth.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        // A brand-new user has no activity, so personalized falls back to the featured
        // catalogue. The response may be capped by `limit`, so every returned name must
        // come from the featured list instead of comparing raw sizes.
        List<String> featuredNames = new ArrayList<>();
        featured.forEach(n -> featuredNames.add(n.path("name").asText()));
        List<String> recommendedNames = new ArrayList<>();
        recommended.forEach(n -> recommendedNames.add(n.path("name").asText()));
        assertThat(recommendedNames)
                .as("a new user's personalized list falls back to featured destinations")
                .isNotEmpty()
                .isSubsetOf(featuredNames);
        // The request itself caps the list at 4; the response must honor that cap.
        assertThat(recommended.size())
                .as("the limit=4 request parameter is honored")
                .isLessThanOrEqualTo(4);
    }

    @Test
    void userWithSeaPlansIsRankedTowardSeaDestinations() throws Exception {
        JsonNode auth = loginGoogle("reco-sea-" + System.nanoTime() + "@voyanta.test", "Sea Lover");
        String token = auth.path("accessToken").asText();
        String sessionId = completeSurveyWithInterests(
                createSurveySession().path("sessionId").asText(), "[\"SEA\"]");
        UUID planId = generatePlan(sessionId, null, uniqueIp());
        mockMvc.perform(withAuth(post("/api/plans/" + planId + "/claim"), token))
                .andExpect(status().isOk());

        JsonNode recommended = json(mockMvc.perform(
                        withAuth(get("/api/recommendations/personalized").param("limit", "2"), token))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        List<String> names = new ArrayList<>();
        recommended.forEach(n -> names.add(n.path("name").asText()));
        assertThat(names).containsAnyOf("Santorini", "Maldiv adaları");
        assertThat(names).doesNotContain("Misir");
    }
}
