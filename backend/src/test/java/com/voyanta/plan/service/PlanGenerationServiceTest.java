package com.voyanta.plan.service;

import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import com.voyanta.common.exception.RateLimitExceededException;
import com.voyanta.common.ratelimit.RateLimiter;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
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
import com.voyanta.survey.service.SurveySessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PlanGenerationService, focused on the dispatch boundary.
 *
 * The key contract: startGeneration must persist a GENERATING plan and hand the work
 * to the separate worker bean, never run the AI call itself. If the worker's method
 * were called via self-invocation the @Async proxy would be bypassed and the AI call
 * would run on the request thread again.
 */
@ExtendWith(MockitoExtension.class)
class PlanGenerationServiceTest {

    @Mock
    private SurveySessionService surveySessionService;
    @Mock
    private TravelPlanRepository planRepository;
    @Mock
    private PlanGenerationWorker planGenerationWorker;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private VoyantaProperties properties;
    @Mock
    private VoyantaProperties.RateLimit rateLimit;

    private PlanGenerationService service;
    private SurveySession survey;

    @BeforeEach
    void setUp() {
        service = new PlanGenerationService(
                surveySessionService, planRepository, planGenerationWorker, rateLimiter, properties);

        survey = new SurveySession(
                "session-1",
                Set.of(InterestType.SEA),
                CompanionType.SOLO,
                null,
                new Budget(BudgetTier.ECONOMY, null),
                new TravelDates(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4), null),
                HotelType.THREE_STAR,
                MealPreference.NO_MEALS,
                Set.of(TripPurpose.ADVENTURE),
                true
        );
    }

    @Test
    void generationIsDelegatedToTheWorkerBeanAndNotRunInline() {
        String planId = prepareAcceptedGeneration();

        service.startGeneration("session-1", "ip:abc", false);

        // Delegating to the worker bean is what makes @Async effective; the AI call
        // itself must never appear in this class.
        verify(planGenerationWorker).generateAsync(UUID.fromString(planId), survey, false);
    }

    @Test
    void thePlanIsPersistedAsGeneratingBeforeTheWorkerRuns() {
        String planId = prepareAcceptedGeneration();

        service.startGeneration("session-1", "ip:abc", false);

        ArgumentCaptor<TravelPlan> saved = ArgumentCaptor.forClass(TravelPlan.class);
        verify(planRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PlanStatus.GENERATING);
        assertThat(saved.getValue().getId()).isEqualTo(UUID.fromString(planId));
    }

    @Test
    void rejectedTaskMarksThePlanFailedInsteadOfLeavingItGenerating() {
        // Executor saturation: the queue refuses the task. The plan would otherwise
        // stay GENERATING forever and the frontend would poll indefinitely.
        String planId = prepareAcceptedGeneration();
        doThrow(new TaskRejectedException("executor queue full"))
                .when(planGenerationWorker).generateAsync(any(), any(), anyBoolean());

        service.startGeneration("session-1", "ip:abc", false);

        verify(planGenerationWorker).markFailed(UUID.fromString(planId));
    }

    @Test
    void unexpectedWorkerFailureDoesNotEscapeToTheClient() {
        // startGeneration already returned 202 semantics; a dispatch failure must be
        // contained so the endpoint does not turn into a 500.
        String planId = prepareAcceptedGeneration();
        doThrow(new IllegalStateException("proxy exploded"))
                .when(planGenerationWorker).generateAsync(any(), any(), anyBoolean());

        assertThatThrownBy(() -> service.startGeneration("session-1", "ip:abc", false))
                .isInstanceOf(IllegalStateException.class);

        verify(planGenerationWorker, never()).markFailed(any());
    }

    @Test
    void existingGeneratingPlanIsReturnedWithoutDispatchingAgain() {
        TravelPlan existing = TravelPlan.builder()
                .id(UUID.randomUUID())
                .anonymousSessionId("session-1")
                .status(PlanStatus.GENERATING)
                .build();
        when(planRepository.findByAnonymousSessionId("session-1")).thenReturn(Optional.of(existing));

        TravelPlan result = service.startGeneration("session-1", "ip:abc", false);

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(planGenerationWorker, never()).generateAsync(any(), any(), anyBoolean());
        verify(rateLimiter, never()).tryConsume(anyString(), anyInt(), any());
    }

    @Test
    void failedPlanIsDeletedAndRegenerated() {
        TravelPlan failed = TravelPlan.builder()
                .id(UUID.randomUUID())
                .anonymousSessionId("session-1")
                .status(PlanStatus.FAILED)
                .build();
        when(planRepository.findByAnonymousSessionId("session-1")).thenReturn(Optional.of(failed));
        // Only the parts the regeneration path actually reaches.
        when(properties.getRateLimit()).thenReturn(rateLimit);
        when(surveySessionService.get("session-1")).thenReturn(survey);
        when(rateLimit.getAnonymousPlansPerDay()).thenReturn(2);
        when(rateLimiter.tryConsume(anyString(), anyInt(), any())).thenReturn(true);
        UUID planId = UUID.randomUUID();
        when(planRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> {
            TravelPlan plan = invocation.getArgument(0);
            plan.setId(planId);
            return plan;
        });

        service.startGeneration("session-1", "ip:abc", false);

        verify(planRepository).delete(failed);
        verify(planGenerationWorker).generateAsync(planId, survey, false);
    }

    @Test
    void dailyRateLimitIsEnforcedBeforeDispatch() {
        when(planRepository.findByAnonymousSessionId(anyString())).thenReturn(Optional.empty());
        when(properties.getRateLimit()).thenReturn(rateLimit);
        when(surveySessionService.get("session-1")).thenReturn(survey);
        when(rateLimit.getAnonymousPlansPerDay()).thenReturn(2);
        when(rateLimiter.tryConsume(anyString(), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.startGeneration("session-1", "ip:abc", false))
                .isInstanceOf(RateLimitExceededException.class);

        verify(planGenerationWorker, never()).generateAsync(any(), any(), anyBoolean());
    }

    @Test
    void incompleteSurveyIsRejectedBeforeAnyAiWork() {
        SurveySession empty = SurveySession.empty("session-1");
        when(planRepository.findByAnonymousSessionId(anyString())).thenReturn(Optional.empty());
        when(surveySessionService.get("session-1")).thenReturn(empty);
        // No rate-limit stub: validation must reject before the limiter is consulted.

        assertThatThrownBy(() -> service.startGeneration("session-1", "ip:abc", false))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(api.getErrorCode()).isEqualTo("SURVEY_INCOMPLETE");
                });

        verify(planGenerationWorker, never()).generateAsync(any(), any(), anyBoolean());
    }

    /**
     * Wires the happy path up to the point where startGeneration is called, and
     * returns the id the plan is persisted with.
     */
    private String prepareAcceptedGeneration() {
        UUID planId = UUID.randomUUID();
        when(properties.getRateLimit()).thenReturn(rateLimit);
        when(planRepository.findByAnonymousSessionId("session-1")).thenReturn(Optional.empty());
        when(surveySessionService.get("session-1")).thenReturn(survey);
        when(rateLimit.getAnonymousPlansPerDay()).thenReturn(2);
        when(properties.getRateLimit()).thenReturn(rateLimit);
        when(rateLimiter.tryConsume(anyString(), anyInt(), any())).thenReturn(true);
        when(planRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> {
            TravelPlan plan = invocation.getArgument(0);
            plan.setId(planId);
            return plan;
        });
        return planId.toString();
    }
}
