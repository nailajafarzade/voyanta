package com.voyanta.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanGenerationFlowIT extends AbstractIntegrationTest {

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Test
    void generatePersistsReadyPlanAndLocksDaysForAnonymousUsers() throws Exception {
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        UUID planId = generatePlan(sessionId, null, uniqueIp());

        // Generation is asynchronous now, so poll until the plan reaches a terminal state.
        JsonNode status = awaitPlanReady(planId);
        assertThat(status.path("message").asText()).isEqualTo("Planın hazırdır");

        JsonNode plan = json(mockMvc.perform(get("/api/plans/" + planId))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(plan.path("destination").asText()).isEqualTo("Santorini");
        assertThat(plan.path("status").asText()).isEqualTo("READY");
        assertThat(plan.path("days")).hasSize(3);
        assertThat(plan.path("days").get(0).path("locked").asBoolean()).isFalse();
        assertThat(plan.path("days").get(1).path("locked").asBoolean()).isFalse();
        assertThat(plan.path("days").get(2).path("locked").asBoolean()).isTrue();
        assertThat(plan.path("days").get(2).path("items").isMissingNode()
                || plan.path("days").get(2).path("items").isNull()).isTrue();
        assertThat(plan.path("budgetSummary").path("total").decimalValue()).isNotNull();

        assertThat(travelPlanRepository.findById(planId)).isPresent();
        assertThat(travelPlanRepository.findById(planId).orElseThrow().getStatus()).isEqualTo(PlanStatus.READY);
    }

    @Test
    void generateIsIdempotentForTheSameSurveySession() throws Exception {
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        String ip = uniqueIp();
        UUID first = generatePlan(sessionId, null, ip);
        UUID second = generatePlan(sessionId, null, ip);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void incompleteSurveyIsRejected() throws Exception {
        String sessionId = createSurveySession().path("sessionId").asText();
        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", uniqueIp())
                        .content("{\"surveySessionId\":\"" + sessionId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("SURVEY_INCOMPLETE"));
    }

    @Test
    void missingSurveySessionIsNotFound() throws Exception {
        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", uniqueIp())
                        .content("{\"surveySessionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("RESOURCE_NOT_FOUND"));
    }

    @Test
    void blankSurveySessionIdFailsValidation() throws Exception {
        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"surveySessionId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void failedGenerationCanBeRetried() throws Exception {
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        String ip = uniqueIp();
        fakeAiPlanGenerator.failNextCalls();
        UUID failedId = generatePlan(sessionId, null, ip);

        awaitPlanFailed(failedId);

        fakeAiPlanGenerator.reset();
        UUID retriedId = generatePlan(sessionId, null, ip);
        assertThat(retriedId).isNotEqualTo(failedId);
        awaitPlanReady(retriedId);
        assertThat(travelPlanRepository.findById(failedId)).isEmpty();
    }

    @Test
    void anonymousDailyLimitIsEnforcedPerIp() throws Exception {
        String ip = uniqueIp();
        UUID first = generatePlan(completeSurvey(createSurveySession().path("sessionId").asText()), null, ip);
        UUID second = generatePlan(completeSurvey(createSurveySession().path("sessionId").asText()), null, ip);
        assertThat(first).isNotEqualTo(second);

        String thirdSession = completeSurvey(createSurveySession().path("sessionId").asText());
        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content("{\"surveySessionId\":\"" + thirdSession + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void authenticatedGenerationUnlocksEveryDay() throws Exception {
        JsonNode auth = loginGoogle("plan-" + System.nanoTime() + "@voyanta.test", "Planner");
        String token = auth.path("accessToken").asText();
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        UUID planId = generatePlan(sessionId, token, uniqueIp());
        awaitPlanReady(planId);

        JsonNode plan = json(mockMvc.perform(withAuth(get("/api/plans/" + planId), token))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        for (JsonNode day : plan.path("days")) {
            assertThat(day.path("locked").asBoolean()).isFalse();
            assertThat(day.path("items").isArray()).isTrue();
        }
    }

    @Test
    void claimAttachesAnonymousPlanAndUnlocksDays() throws Exception {
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        UUID planId = generatePlan(sessionId, null, uniqueIp());
        awaitPlanReady(planId);

        mockMvc.perform(post("/api/plans/" + planId + "/claim"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("AUTH_REQUIRED"));

        JsonNode owner = loginGoogle("claim-owner-" + System.nanoTime() + "@voyanta.test", "Owner");
        String ownerToken = owner.path("accessToken").asText();
        mockMvc.perform(withAuth(post("/api/plans/" + planId + "/claim"), ownerToken))
                .andExpect(status().isOk());

        JsonNode owned = json(mockMvc.perform(withAuth(get("/api/plans/" + planId), ownerToken))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(owned.path("days").get(2).path("locked").asBoolean()).isFalse();
        assertThat(owned.path("days").get(2).path("items").isArray()).isTrue();

        JsonNode other = loginGoogle("claim-other-" + System.nanoTime() + "@voyanta.test", "Other");
        mockMvc.perform(withAuth(post("/api/plans/" + planId + "/claim"), other.path("accessToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("PLAN_ALREADY_CLAIMED"));
    }

    @Test
    void missingPlanReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/plans/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(json(result).path("errorCode").asText())
                        .isEqualTo("RESOURCE_NOT_FOUND"));
    }
}
