package com.voyanta.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.exception.ResourceNotFoundException;
import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.image.DestinationImageQuery;
import com.voyanta.image.ImageService;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.dto.response.ItineraryDayResponse;
import com.voyanta.plan.dto.response.PlanResponse;
import com.voyanta.plan.dto.response.PlanStatusResponse;
import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for reading plans. It builds the polling status with a progress
 * message, and returns the full plan while hiding the activities of locked days from
 * anyone who does not own the plan.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanQueryService {

    private static final String PROGRESS_KEY_PREFIX = "plan:progress:";

    private final TravelPlanRepository planRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ImageService imageService;
    private final DestinationRepository destinationRepository;

    public PlanStatusResponse getStatus(UUID planId) {
        TravelPlan plan = findPlan(planId);

        if (plan.getStatus() != PlanStatus.GENERATING) {
            return new PlanStatusResponse(plan.getStatus(), null, messageFor(plan.getStatus(), null));
        }

        GenerationStage stage = readStage(PROGRESS_KEY_PREFIX + planId);
        return new PlanStatusResponse(plan.getStatus(), stage, messageFor(plan.getStatus(), stage));
    }

    /**
     * Reads the progress stage back from Redis.
     *
     * Redis value is written as JSON by GenericJackson2JsonRedisSerializer, so on the way
     * back it arrives as a String (or a Map), NOT as a GenerationStage instance. A direct
     * cast therefore throws ClassCastException and the status endpoint returns 500.
     *
     * This was previously masked: generation ran synchronously on the request thread, so
     * a plan was already READY/FAILED by the time anybody polled its status and this
     * GENERATING branch was never hit. Now that generation is properly asynchronous this
     * is the normal path — the frontend polls while the plan is still building — so the
     * conversion has to be done explicitly. Same approach as SurveySessionService.get().
     */
    private GenerationStage readStage(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, GenerationStage.class);
        } catch (IllegalArgumentException e) {
            // Yarım yazılmış/naməlum dəyər status sorğusunu çökmətməlidir.
            log.warn("Plan progress key-i oxunmadı: {}", key, e);
            return null;
        }
    }

    @Transactional
    public PlanResponse getPlan(UUID planId, UUID requestingUserId) {
        TravelPlan plan = findPlan(planId);

        // Defense in depth: DB-dəki locked bayrağına etibar etməklə yanaşı, məhz burada
        // da bir daha yoxlayırıq ki, sahiblik olmadan heç kim items görməsin.
        boolean canSeeAll = requestingUserId != null && requestingUserId.equals(plan.getUserId());

        List<ItineraryDayResponse> days = plan.getDays().stream()
                .map(day -> toDayResponse(day, canSeeAll))
                .collect(Collectors.toList());

        List<ImageCandidateResponse> images = resolveImages(plan);
        // images boşdursa cover = null: bu, plan üçün heç bir DB sütunu yoxdur,
        // response non_null ilə seriya olunanda sadəcə sahə olmur.
        String cover = images.isEmpty() ? null : images.get(0).url();

        UUID destinationId = resolveDestinationId(plan, cover);

        return new PlanResponse(
                plan.getId(), plan.getDestination(), destinationId, cover, images,
                plan.getStartDate(), plan.getEndDate(),
                plan.getCompanion(), plan.getBudgetSummary(), plan.getStatus(), days
        );
    }

    /**
     * Destination cover image candidates for the plan card.
     *
     * The plan has exactly ONE destination (TravelPlan.destination), so this resolves
     * that single name; the itinerary days below it are activities, not destinations.
     *
     * <p>Keçmişdə TƏK URL qaytarılırdı. İndi bütün relevant namizədlər qaytarılır ki,
     * frontend tam ekran baxış üçün seçim təklif edə bilsin. AI generasiyası bu
     * axının bir hissəsi DEYİL — dəyişən yalnız şəkil qatıdır.
     *
     * ImageService never throws - a missing image simply yields an empty list here, the
     * same value a GENERATING plan already returns for other not-yet-set fields, and the
     * response is serialized with non_null inclusion so the field is simply absent.
     */
    private List<ImageCandidateResponse> resolveImages(TravelPlan plan) {
        String destination = plan.getDestination();
        if (destination == null || destination.isBlank()) {
            return List.of();
        }
        // Sorğunu yalnız adla qurmaq əvvəl zəif idi ("Kahire" -> ümumi şəkil).
        // Planın öz maraq teqləri varsa (təbiət/dəniz/tarix), onu da keçirik:
        // beləcə plan örtüyü də eyni ağıllı seçimdən keçir.
        return imageService.resolveImageCandidates(
                DestinationImageQuery.of(destination, null, null, plan.getInterests()));
    }

    private ItineraryDayResponse toDayResponse(ItineraryDay day, boolean canSeeAll) {
        boolean locked = day.isLocked() && !canSeeAll;
        return new ItineraryDayResponse(day.getDayNumber(), locked, locked ? null : day.getItems());
    }

    private TravelPlan findPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan tapılmadı"));
    }

    private String messageFor(PlanStatus status, GenerationStage stage) {
        if (status == PlanStatus.FAILED) return "Plan hazırlanarkən xəta baş verdi";
        if (status == PlanStatus.READY) return "Planın hazırdır";
        if (stage == null) return "Plan hazırlanır...";

        return switch (stage) {
            case ANALYZING_INTERESTS -> "Maraq dairən təhlil olundu";
            case SELECTING_PLACES -> "Uyğun məkanlar seçildi";
            case BUILDING_ITINERARY -> "Gündəlik marşrut qurulur...";
            case DONE -> "Planın hazırdır";
        };
    }

    static final String PLAN_TAG = "Səyahət planı";

    private UUID resolveDestinationId(TravelPlan plan, String cover) {
        String raw = plan.getDestination();
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        int comma = trimmed.indexOf(',');
        String city = (comma > 0 ? trimmed.substring(0, comma) : trimmed).trim();
        String country = comma > 0 ? trimmed.substring(comma + 1).trim() : "";
        if (city.isEmpty() || city.length() > 100) return null;

        Optional<Destination> existing = destinationRepository
                .findFirstByNameIgnoreCaseOrderByCreatedAtAsc(trimmed)
                .or(() -> destinationRepository.findFirstByNameIgnoreCaseOrderByCreatedAtAsc(city));
        if (existing.isPresent()) return existing.get().getId();

        if (plan.getStatus() != PlanStatus.READY || cover == null) return null;

        return destinationRepository.save(Destination.builder()
                .name(city).country(country).imageUrl(cover).tag(PLAN_TAG).build()).getId();
    }
}