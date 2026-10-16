package com.voyanta.plan.service;

import com.voyanta.ai.AiPlanGenerator;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import com.voyanta.common.config.AsyncConfig;
import com.voyanta.common.exception.ApiException;
import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.dto.response.SurveySession;
import com.voyanta.survey.enums.BudgetTier;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the background plan-generation worker.
 *
 * The production bug these protect against: plan generation used to run
 * synchronously on the HTTP request thread, so a slow or rate-limited OpenAI call
 * held a Tomcat worker for the whole call (up to 15s x 3 attempts). That is what
 * let an AI outage make the whole API, including Google sign-in, unresponsive.
 */
@ExtendWith(MockitoExtension.class)
class PlanGenerationWorkerTest {

    @Mock
    private AiPlanGenerator aiPlanGenerator;
    @Mock
    private TravelPlanRepository planRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private PlanLockingService lockingService;
    @InjectMocks
    private PlanGenerationWorker worker;

    private SurveySession survey;

    @BeforeEach
    void setUp() {
        survey = new SurveySession(
                "session-1",
                Set.of(InterestType.SEA),
                CompanionType.COUPLE,
                null,
                new Budget(BudgetTier.MID_RANGE, null),
                new TravelDates(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), null),
                HotelType.FOUR_STAR,
                MealPreference.BREAKFAST_INCLUDED,
                Set.of(TripPurpose.RELAXATION),
                true
        );
    }

    @Test
    void generateAsyncIsAnnotatedForAsynchronousExecutionOnTheBoundedExecutor() throws Exception {
        // The @Async annotation is the whole mechanism. If it is dropped, removed from
        // this bean, or points at a different executor, the AI call silently falls back
        // to the caller's (request) thread and the original outage returns.
        // Purely reflective: no Spring context and no mock interactions.
        Async async = AnnotationUtils.findAnnotation(PlanGenerationWorker.class.getMethod(
                "generateAsync", UUID.class, SurveySession.class, boolean.class), Async.class);

        assertThat(async)
                .as("@Async must stay on the worker: without it generation blocks the request thread")
                .isNotNull();
        assertThat(async.value())
                .as("the bounded executor is what keeps an AI flood from exhausting the server")
                .isEqualTo(AsyncConfig.PLAN_GENERATION_EXECUTOR);
    }
    @Test
    void successfulGenerationMarksThePlanReadyAndPersistsTheItinerary() {
        UUID planId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        TravelPlan plan = generating(planId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(aiPlanGenerator.generate(any())).thenReturn(sampleResponse());

        worker.generateAsync(planId, survey, false);

        ArgumentCaptor<TravelPlan> saved = ArgumentCaptor.forClass(TravelPlan.class);
        verify(planRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PlanStatus.READY);
        assertThat(saved.getValue().getDestination()).isEqualTo("Santorini");
        assertThat(saved.getValue().getDays()).hasSize(2);
        verify(lockingService).applyLocking(plan, false);
    }

    @Test
    void successfulGenerationPublishesProgressStagesInOrder() {
        UUID planId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findById(planId)).thenReturn(Optional.of(generating(planId)));
        when(aiPlanGenerator.generate(any())).thenReturn(sampleResponse());

        worker.generateAsync(planId, survey, true);

        ArgumentCaptor<Object> stages = ArgumentCaptor.forClass(Object.class);
        verify(valueOperations, atLeastOnce()).set(anyString(), stages.capture(), any(Duration.class));
        assertThat(stages.getAllValues())
                .extracting(Object::toString)
                .containsSubsequence(
                        GenerationStage.ANALYZING_INTERESTS.name(),
                        GenerationStage.SELECTING_PLACES.name(),
                        GenerationStage.BUILDING_ITINERARY.name(),
                        GenerationStage.DONE.name());
    }

    @Test
    void aiRateLimitFailureMarksThePlanFailedInsteadOfPropagating() {
        // This is the production scenario: OpenAI returns 429. The worker must swallow
        // it and mark the plan FAILED. It must NOT rethrow, because an escaping
        // exception on the executor thread would be an unhandled async error.
        UUID planId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        TravelPlan plan = generating(planId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE", "rate limited"))
                .when(aiPlanGenerator).generate(any());

        worker.generateAsync(planId, survey, false);

        assertThat(plan.getStatus()).isEqualTo(PlanStatus.FAILED);
        verify(planRepository).save(plan);
    }

    @Test
    void aiFailureDoesNotOverwriteAnAlreadyReadyPlanWithGarbage() {
        // markFailed must be idempotent and must not run twice for one failure.
        UUID planId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        TravelPlan plan = generating(planId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        doThrow(new IllegalStateException("boom")).when(aiPlanGenerator).generate(any());

        worker.generateAsync(planId, survey, false);

        assertThat(plan.getStatus()).isEqualTo(PlanStatus.FAILED);
        verify(planRepository, times(1)).save(any());
    }

    @Test
    void markFailedIsSafeWhenThePlanNoLongerExists() {
        // Happens if the plan was deleted (e.g. a retry replaced it) between dispatch
        // and execution. Must not throw.
        UUID planId = UUID.randomUUID();
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        worker.markFailed(planId);

        verify(planRepository, never()).save(any());
    }
    @Test
    void aiIsCalledWithTheSurveyData() {
        UUID planId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(planRepository.findById(planId)).thenReturn(Optional.of(generating(planId)));
        when(aiPlanGenerator.generate(any())).thenReturn(sampleResponse());

        worker.generateAsync(planId, survey, false);

        ArgumentCaptor<AiRequest> request = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiPlanGenerator).generate(request.capture());
        assertThat(request.getValue().interests()).containsExactly(InterestType.SEA);
        assertThat(request.getValue().companion()).isEqualTo(CompanionType.COUPLE);
        assertThat(request.getValue().hotelType()).isEqualTo(HotelType.FOUR_STAR);
    }

    @Test
    void failureInsideApplyResponseAlsoMarksThePlanFailed() {
        // Guards the whole try/catch, not just the AI call itself.
        UUID planId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        TravelPlan plan = generating(planId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(aiPlanGenerator.generate(any())).thenReturn(sampleResponse());
        doThrow(new IllegalStateException("db exploded")).when(lockingService).applyLocking(any(), any(Boolean.class));

        worker.generateAsync(planId, survey, false);

        assertThat(plan.getStatus()).isEqualTo(PlanStatus.FAILED);
    }

    private static TravelPlan generating(UUID planId) {
        return TravelPlan.builder().id(planId).status(PlanStatus.GENERATING).build();
    }

    private static AiResponse sampleResponse() {
        return new AiResponse(
                "Santorini",
                List.of(
                        new AiResponse.AiDayPlan(1, List.of(
                                new AiResponse.AiItineraryItem("09:00", "Old town", "Walk", "activity"))),
                        new AiResponse.AiDayPlan(2, List.of(
                                new AiResponse.AiItineraryItem("10:00", "Beach", "Swim", "leisure")))
                ),
                new AiResponse.AiBudgetBreakdown(
                        new BigDecimal("400"), new BigDecimal("150"),
                        new BigDecimal("80"), new BigDecimal("120"), new BigDecimal("750"))
        );
    }
}
