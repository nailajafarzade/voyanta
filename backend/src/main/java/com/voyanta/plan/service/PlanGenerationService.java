package com.voyanta.plan.service;

import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import com.voyanta.common.exception.RateLimitExceededException;
import com.voyanta.common.ratelimit.RateLimiter;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.dto.response.SurveySession;
import com.voyanta.survey.service.SurveySessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Request-facing half of plan generation: idempotency, the daily rate limit,
 * persisting the GENERATING plan, and handing the actual work to
 * {@link PlanGenerationWorker}, which runs it on a background executor.
 *
 * The split is deliberate. AI generation is a blocking outbound HTTP call that can
 * take many seconds or come back with a provider rate limit (429). Running it on
 * the request thread meant a slow or rate-limited AI provider held Tomcat worker
 * threads hostage, so the whole API - including the public auth endpoints - became
 * unresponsive. This method now returns 202 immediately and the AI work happens on
 * a bounded background executor (see AsyncConfig).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanGenerationService {

    private final SurveySessionService surveySessionService;
    private final TravelPlanRepository planRepository;
    private final PlanGenerationWorker planGenerationWorker;
    private final RateLimiter rateLimiter;
    private final VoyantaProperties properties;

    public TravelPlan startGeneration(String surveySessionId, String rateLimitKey, boolean isAuthenticated) {
        // 1) Idempotency ONCE validated - a repeated request must not eat the rate-limit budget
        TravelPlan existing = planRepository.findByAnonymousSessionId(surveySessionId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() != PlanStatus.FAILED) {
                return existing;
            }
            // Failed plan: delete it and start over
            planRepository.delete(existing);
            planRepository.flush();
        }

        SurveySession survey = surveySessionService.get(surveySessionId);
        validateComplete(survey);

        int dailyLimit = isAuthenticated
                ? properties.getRateLimit().getAuthenticatedPlansPerDay()
                : properties.getRateLimit().getAnonymousPlansPerDay();

        if (!rateLimiter.tryConsume(rateLimitKey, dailyLimit, Duration.ofDays(1))) {
            throw new RateLimitExceededException("Gündəlik plan limitinə çatmısan");
        }

        surveySessionService.markCompleted(surveySessionId);

        TravelPlan plan = TravelPlan.builder()
                .anonymousSessionId(surveySessionId)
                .companion(survey.companion())
                .interests(survey.interests())
                .startDate(survey.dates().startDate())
                .endDate(survey.dates().endDate())
                .status(PlanStatus.GENERATING)
                .build();
        planRepository.save(plan);

        dispatch(plan.getId(), survey, isAuthenticated);
        return plan;
    }

    /**
     * Hands the plan over to the background worker.
     *
     * The worker is a SEPARATE bean on purpose: {@code @Async} goes through a Spring
     * AOP proxy, so a self-invocation inside this same bean would bypass it and the
     * AI call would silently run on the request thread again.
     *
     * If the bounded executor is full the task can be rejected. The plan must not stay
     * GENERATING forever in that case, so it is marked FAILED right away.
     */
    private void dispatch(UUID planId, SurveySession survey, boolean isAuthenticated) {
        try {
            planGenerationWorker.generateAsync(planId, survey, isAuthenticated);
        } catch (TaskRejectedException e) {
            log.error("Plan generation could not be queued, planId={}", planId, e);
            planGenerationWorker.markFailed(planId);
        }
    }

    private void validateComplete(SurveySession survey) {
        if (survey.interests() == null || survey.interests().isEmpty()
                || survey.companion() == null || survey.budget() == null || survey.dates() == null
                || survey.hotelType() == null || survey.mealPreference() == null
                || survey.tripPurpose() == null || survey.tripPurpose().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SURVEY_INCOMPLETE", "Sorğu tam doldurulmayıb");
        }
    }
}
