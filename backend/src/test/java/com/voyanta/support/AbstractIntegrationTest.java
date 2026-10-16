package com.voyanta.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;

    static {
        TestcontainersDockerSupport.install();
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("voyanta")
                .withUsername("voyanta")
                .withPassword("voyanta")
                .withStartupTimeout(Duration.ofMinutes(2));
        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("sentry.enabled", () -> "false");
        registry.add("voyanta.oauth.google.client-id", () -> "");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FakeAiPlanGenerator fakeAiPlanGenerator;

    @BeforeEach
    void resetAiStub() {
        fakeAiPlanGenerator.reset();
        // The OAuth failure mode is static state on the shared verifier stub; leaving it
        // set would make an unrelated test see a 503 and fail confusingly.
        FakeGoogleTokenVerifier.reset();
    }

    /**
     * Reads the response body as JSON.
     *
     * The charset is passed explicitly because MockHttpServletResponse.getContentAsString()
     * falls back to ISO-8859-1, while Jackson writes UTF-8 bytes (which is what RFC 8259
     * mandates for application/json, and what every real client assumes). Without this, the
     * Azerbaijani characters in user-facing messages come back as mojibake in tests only.
     */
    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder, String accessToken) {
        if (accessToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return builder;
    }

    protected JsonNode createSurveySession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/survey/sessions"))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data");
    }

    protected String completeSurvey(String sessionId) throws Exception {
        String body = """
                {
                  "interests": ["SEA", "NATURE"],
                  "companion": "COUPLE",
                  "budget": { "tier": "MID_RANGE" },
                  "dates": { "startDate": "2026-10-01", "endDate": "2026-10-05" },
                  "hotelType": "FOUR_STAR",
                  "mealPreference": "BREAKFAST_INCLUDED",
                  "tripPurpose": ["RELAXATION"]
                }
                """;
        mockMvc.perform(patch("/api/survey/sessions/" + sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        return sessionId;
    }

    protected String completeSurveyWithInterests(String sessionId, String interestsJsonArray) throws Exception {
        String body = """
                {
                  "interests": %s,
                  "companion": "SOLO",
                  "budget": { "tier": "ECONOMY" },
                  "dates": { "startDate": "2026-11-01", "endDate": "2026-11-04" },
                  "hotelType": "THREE_STAR",
                  "mealPreference": "NO_MEALS",
                  "tripPurpose": ["ADVENTURE"]
                }
                """.formatted(interestsJsonArray);
        mockMvc.perform(patch("/api/survey/sessions/" + sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        return sessionId;
    }

    protected UUID generatePlan(String surveySessionId, String accessToken, String forwardedFor) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"surveySessionId\":\"" + surveySessionId + "\"}");
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        MvcResult result = mockMvc.perform(withAuth(request, accessToken))
                .andExpect(status().isAccepted())
                .andReturn();
        return UUID.fromString(json(result).path("data").asText());
    }

    protected JsonNode loginGoogle(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid:" + email + ":" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data");
    }

    /**
     * Polls the plan status endpoint until the plan reaches a terminal state
     * (READY or FAILED).
     *
     * Generation runs on a background executor (see PlanGenerationWorker), so a test
     * can no longer assume the plan is finished the moment POST /generate returns 202.
     * Tests that only need "generate was accepted" do not need this helper.
     */
    protected JsonNode awaitPlanStatus(UUID planId, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        JsonNode last = null;

        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/plans/" + planId + "/status"))
                    .andExpect(status().isOk())
                    .andReturn();
            last = json(result).path("data");
            String value = last.path("status").asText();
            if ("READY".equals(value) || "FAILED".equals(value)) {
                return last;
            }
            Thread.sleep(50);
        }

        throw new AssertionError("Plan " + planId + " terminal status-a keçmədi, son status: " + last);
    }

    protected JsonNode awaitPlanReady(UUID planId) throws Exception {
        JsonNode status = awaitPlanStatus(planId, 15_000);
        assertThat(status.path("status").asText())
                .withFailMessage("Plan gözlənilən vaxt ərzində READY olmadı, status=%s", status)
                .isEqualTo("READY");
        return status;
    }

    protected JsonNode awaitPlanFailed(UUID planId) throws Exception {
        JsonNode status = awaitPlanStatus(planId, 15_000);
        assertThat(status.path("status").asText())
                .withFailMessage("Plan gözlənilən vaxt ərzində FAILED olmadı, status=%s", status)
                .isEqualTo("FAILED");
        return status;
    }

    protected String uniqueIp() {
        return UUID.randomUUID().toString();
    }
}
