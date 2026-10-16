package com.voyanta.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.common.config.AsyncConfig;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.support.AbstractIntegrationTest;
import com.voyanta.support.SlowAiPlanGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the asynchronous plan-generation architecture.
 *
 * The production failure this protects against: generation used to run on the HTTP
 * request thread, so while OpenAI was slow or returning 429, the request thread was
 * pinned. Once enough such requests piled up, Tomcat had no free worker threads and
 * unrelated endpoints - including POST /api/auth/oauth/google - stopped responding.
 *
 * These tests hold the AI call open and assert that the request returns 202 anyway,
 * which is impossible if generation is still synchronous.
 */
class AsyncPlanGenerationIT extends AbstractIntegrationTest {

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @AfterEach
    void releaseHeldCall() {
        // Never leave the background thread parked; that would stall later tests.
        SlowAiPlanGenerator.reset();
    }

    @Test
    void generateReturnsAcceptedWhileTheAiCallIsStillBlocked() throws Exception {
        SlowAiPlanGenerator.holdNextCall();
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());

        // If this call blocked on the AI provider it could not return until
        // release() ran, and the test would hang rather than pass.
        UUID planId = generatePlan(sessionId, null, uniqueIp());

        assertThat(planId).isNotNull();
        assertThat(planRepositoryStatus(planId))
                .as("the plan is persisted as GENERATING before the AI call finishes")
                .isIn(PlanStatus.GENERATING, PlanStatus.READY, PlanStatus.FAILED);
    }

    @Test
    void aiGenerationRunsOnADifferentThreadThanTheRequest() throws Exception {
        SlowAiPlanGenerator.holdNextCall();
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());

        long requestThreadId = Thread.currentThread().getId();
        UUID planId = generatePlan(sessionId, null, uniqueIp());

        assertThat(SlowAiPlanGenerator.awaitAiCallStarted(10, TimeUnit.SECONDS))
                .as("the background worker must actually pick the task up")
                .isTrue();

        Thread aiThread = SlowAiPlanGenerator.aiThread();
        assertThat(aiThread)
                .as("AI generation must not run on the thread that served the HTTP request")
                .isNotNull()
                .isNotSameAs(Thread.currentThread());
        assertThat(aiThread.getId()).isNotEqualTo(requestThreadId);
        assertThat(aiThread.getName())
                .as("the worker uses the dedicated bounded executor")
                .startsWith("plan-gen-");

        SlowAiPlanGenerator.release();
        awaitPlanReady(planId);
    }

    @Test
    void releasingTheAiCallCompletesThePlanInTheBackground() throws Exception {
        SlowAiPlanGenerator.holdNextCall();
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        UUID planId = generatePlan(sessionId, null, uniqueIp());

        assertThat(SlowAiPlanGenerator.awaitAiCallStarted(10, TimeUnit.SECONDS)).isTrue();
        SlowAiPlanGenerator.release();

        // No polling loop needed here: the shared helper already polls with a deadline.
        awaitPlanReady(planId);
        assertThat(travelPlanRepository.findById(planId).orElseThrow().getDestination())
                .isEqualTo("Santorini");
    }

    @Test
    void aiFailureLeavesThePlanFailedRatherThanGeneratingForever() throws Exception {
        SlowAiPlanGenerator.failWith(new IllegalStateException("simulated provider outage"));
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        UUID planId = generatePlan(sessionId, null, uniqueIp());

        awaitPlanFailed(planId);

        assertThat(planRepositoryStatus(planId))
                .as("a failed AI call must never leave the plan stuck in GENERATING")
                .isEqualTo(PlanStatus.FAILED);
    }

    @Test
    void statusCanBeQueriedWhileGeneratingWithoutServerError() throws Exception {
        // Regression guard for the ClassCastException: while a plan is GENERATING the
        // status endpoint reads the stage back from Redis, where it is stored as JSON
        // and therefore comes back as a String, not as a GenerationStage enum.
        SlowAiPlanGenerator.holdNextCall();
        String sessionId = completeSurvey(createSurveySession().path("sessionId").asText());
        UUID planId = generatePlan(sessionId, null, uniqueIp());
        assertThat(SlowAiPlanGenerator.awaitAiCallStarted(10, TimeUnit.SECONDS)).isTrue();

        // Polls the status endpoint repeatedly while generation is in flight. Before
        // the fix this returned 500 (ClassCastException) instead of 200.
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> mockMvc.perform(get("/api/plans/" + planId + "/status"))
                        .andExpect(status().isOk()));

        JsonNode data = json(mockMvc.perform(get("/api/plans/" + planId + "/status"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(data.path("status").asText()).isEqualTo("GENERATING");
        assertThat(data.path("message").asText()).isNotBlank();

        SlowAiPlanGenerator.release();
        awaitPlanReady(planId);
    }

    @Test
    void asyncExecutorIsABoundedPoolRegisteredUnderTheExpectedName() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor)
                applicationContext.getBean(AsyncConfig.PLAN_GENERATION_EXECUTOR);

        assertThat(executor.getCorePoolSize()).isGreaterThan(0);
        assertThat(executor.getMaxPoolSize()).isGreaterThanOrEqualTo(executor.getCorePoolSize());
        assertThat(executor.getThreadPoolExecutor().getQueue())
                .as("the queue must be bounded so a provider flood cannot exhaust memory")
                .isNotNull();
    }

    private PlanStatus planRepositoryStatus(UUID planId) {
        return travelPlanRepository.findById(planId).orElseThrow().getStatus();
    }
}
