package com.voyanta.plan.service;

import com.voyanta.ai.AiPlanGenerator;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import com.voyanta.common.config.AsyncConfig;
import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.dto.shared.BudgetSummary;
import com.voyanta.plan.dto.shared.ItineraryItem;
import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.dto.response.SurveySession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Arxa planda işləyən plan generasiyası — AI provider-ə bloklayan HTTP çağırışı
 * burada baş verir.
 *
 * Niyə AYRI bean: {@code @Async} Spring AOP proxy-si ilə işləyir və yalnız
 * BAŞQA bean-dən çağırıldıqda tətbiq olunur. Əgər bu məntiq
 * {@code PlanGenerationService.startGeneration()} daxilində qalsa (öz beşinə
 * çağırış — self-invocation), proxy keçilmir və @Async səssizcə sönür: AI
 * çağırışı HTTP sorğu thread-ində icra olur. Sorğu o vaxt "202 Accepted"
 * qaytarmalıdır, amma əvəzinə provider-a qədər bloklanırdı.
 *
 * Bu, əvvəlki problemin ikinci yarısı idi: AI çağırışı bloklayan və 429/5xx
 * alanda təkrar cəhd edən tətbiq bütün servisləri (o cümlədən giriş
 * endpoint-lərini) tutmaqla yanaşırdı. İndi AI işi məhdud executor-da gedir və
 * uğursuzluq yalnız planı FAILED edir — heç bir digər endpoint-i toxunmur.
 *
 * Bu klass heç bir autentifikasiya məntiqi daşımır və heç vaxt istifadəçi
 * giriş sorğusuna 401 qaytarmağa yol vermir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanGenerationWorker {

    private static final String PROGRESS_KEY_PREFIX = "plan:progress:";
    private static final Duration PROGRESS_TTL = Duration.ofMinutes(5);

    private final AiPlanGenerator aiPlanGenerator;
    private final TravelPlanRepository planRepository;
    private final PlanLockingService lockingService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Async(AsyncConfig.PLAN_GENERATION_EXECUTOR)
    public void generateAsync(UUID planId, SurveySession survey, boolean isAuthenticated) {
        try {
            updateStage(planId, GenerationStage.ANALYZING_INTERESTS);

            AiRequest request = new AiRequest(
                    survey.interests(), survey.companion(), survey.familyDetails(),
                    survey.budget(), survey.dates(),
                    survey.hotelType(), survey.mealPreference(), survey.tripPurpose()
            );

            updateStage(planId, GenerationStage.SELECTING_PLACES);
            AiResponse response = aiPlanGenerator.generate(request);

            updateStage(planId, GenerationStage.BUILDING_ITINERARY);
            applyAiResponse(planId, response, isAuthenticated);

            updateStage(planId, GenerationStage.DONE);
        } catch (Exception e) {
            // AiPlanGenerator artıq hamısını ApiException-a çevirir (429 -> 503 AI_UNAVAILABLE
            // və s.). Burada yalnız plan vəziyyətini FAILED edirik — xəta heç bir yerə
            // sıçmır, yəni heç bir endpoint-i 500/401 etmir.
            log.error("Plan generasiyası uğursuz oldu, planId={}", planId, e);
            markFailed(planId);
        }
    }

    private void applyAiResponse(UUID planId, AiResponse response, boolean isAuthenticated) {
        TravelPlan plan = planRepository.findById(planId).orElseThrow();

        // Hansı destinasiyanın seçildiyini görünən edir: eyni yerlərin daim
        // təkrarlanması kimi problemlər logdan izlənə bilər.
        log.info("AI plan cavabı qəbul edildi: planId={}, destination={}",
                planId, response.destination());

        plan.setDestination(response.destination());
        plan.setBudgetSummary(toBudgetSummary(response.budgetSummary()));

        List<ItineraryDay> days = response.days().stream()
                .map(d -> ItineraryDay.builder()
                        .plan(plan)
                        .dayNumber(d.dayNumber())
                        .items(toItineraryItems(d.items()))
                        .build())
                .collect(Collectors.toList());

        plan.setDays(days);
        lockingService.applyLocking(plan, isAuthenticated);
        plan.setStatus(PlanStatus.READY);

        planRepository.save(plan);
    }

    /**
     * Executor dolu olduqda tapşırıq rədd olunanda plan sonsuz GENERATING qalmasın deyə
     * çağırılır. Paket-private: PlanGenerationService (eyni paket) işlətdikdən sonra
     * heç nəyi təmizləmir.
     */
    void markFailed(UUID planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            plan.setStatus(PlanStatus.FAILED);
            planRepository.save(plan);
        });
    }

    private void updateStage(UUID planId, GenerationStage stage) {
        redisTemplate.opsForValue().set(PROGRESS_KEY_PREFIX + planId, stage, PROGRESS_TTL);
    }

    // ai.dto.response tiplərini plan-ın öz DTO-larına çevirir — bu mapping bilməkdən
    // burada, plan tərəfindədir ki, ai modulu plan-dan asılı olmasın.
    private BudgetSummary toBudgetSummary(AiResponse.AiBudgetBreakdown b) {
        return new BudgetSummary(b.accommodation(), b.food(), b.transport(), b.activities(), b.total());
    }

    private List<ItineraryItem> toItineraryItems(List<AiResponse.AiItineraryItem> items) {
        return items.stream()
                .map(i -> new ItineraryItem(i.time(), i.title(), i.description(), i.category()))
                .collect(Collectors.toList());
    }
}
