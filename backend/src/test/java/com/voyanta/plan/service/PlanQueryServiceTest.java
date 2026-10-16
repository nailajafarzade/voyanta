package com.voyanta.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.image.DestinationImageQuery;
import com.voyanta.image.ImageService;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.dto.response.PlanResponse;
import com.voyanta.plan.dto.response.PlanStatusResponse;
import com.voyanta.plan.dto.shared.ItineraryItem;
import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.enums.InterestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {

    @Mock
    private TravelPlanRepository planRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ImageService imageService;
    @Mock
    private DestinationRepository destinationRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private PlanQueryService queryService;

    @Test
    void hidesItemsOnLockedDaysUnlessTheRequesterOwnsThePlan() {
        UUID planId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .userId(ownerId)
                .status(PlanStatus.READY)
                .days(List.of(
                        ItineraryDay.builder()
                                .dayNumber(1)
                                .locked(false)
                                .items(List.of(new ItineraryItem("09:00", "Open", "d", "a")))
                                .build(),
                        ItineraryDay.builder()
                                .dayNumber(2)
                                .locked(true)
                                .items(List.of(new ItineraryItem("10:00", "Secret", "d", "a")))
                                .build()
                ))
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        PlanResponse hidden = queryService.getPlan(planId, null);
        assertThat(hidden.days().get(0).items()).isNotNull();
        assertThat(hidden.days().get(1).locked()).isTrue();
        assertThat(hidden.days().get(1).items()).isNull();

        PlanResponse visible = queryService.getPlan(planId, ownerId);
        assertThat(visible.days().get(1).locked()).isFalse();
        assertThat(visible.days().get(1).items()).isNotNull();
    }

    // ------------------------------------------------------------------
    // AI plan image path: the plan cover uses the SAME multi-candidate image
    // layer as the destination cards. AI generation itself is untouched.
    // ------------------------------------------------------------------

    @Test
    void planCoverUsesTheFirstCandidateAndExposesTheWholeList() {
        UUID planId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .destination("Greenland")
                .interests(Set.of(InterestType.NATURE))
                .status(PlanStatus.READY)
                .days(List.of())
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        // A READY plan auto-creates its destination row (resolveDestinationId) — the
        // repository mock must hand back a persisted entity with an id.
        when(destinationRepository.save(any())).thenAnswer(invocation -> {
            Destination created = invocation.getArgument(0);
            created.setId(UUID.randomUUID());
            return created;
        });

        List<ImageCandidateResponse> candidates = List.of(
                new ImageCandidateResponse("a", "https://images.unsplash.com/a", "https://images.unsplash.com/a-full",
                        "Jane", "https://unsplash.com/@jane", "https://unsplash.com/photos/a"),
                new ImageCandidateResponse("b", "https://images.unsplash.com/b", "https://images.unsplash.com/b-full",
                        "John", "https://unsplash.com/@john", "https://unsplash.com/photos/b"));
        when(imageService.resolveImageCandidates(any())).thenReturn(candidates);

        PlanResponse response = queryService.getPlan(planId, null);

        assertThat(response.imageUrl()).isEqualTo("https://images.unsplash.com/a");
        assertThat(response.images()).hasSize(2);

        // The plan's own interests must reach the query so nature plans search
        // for landscapes instead of a generic photo.
        ArgumentCaptor<DestinationImageQuery> query = ArgumentCaptor.forClass(DestinationImageQuery.class);
        verify(imageService).resolveImageCandidates(query.capture());
        assertThat(query.getValue().destination()).isEqualTo("Greenland");
        assertThat(query.getValue().interests()).containsExactly(InterestType.NATURE);
    }

    @Test
    void planWithoutAnyImageCandidateHasNoCoverAndEmptyList() {
        UUID planId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .destination("Nowhere At All")
                .status(PlanStatus.READY)
                .days(List.of())
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(imageService.resolveImageCandidates(any())).thenReturn(List.of());

        PlanResponse response = queryService.getPlan(planId, null);

        assertThat(response.imageUrl()).isNull();
        assertThat(response.images()).isEmpty();
    }

    // ------------------------------------------------------------------
    // Regression test for a real production bug: the progress stage is stored in Redis
    // via GenericJackson2JsonRedisSerializer, so it comes back as a plain String, not as
    // a GenerationStage. A direct cast threw ClassCastException and GET /api/plans/{id}/status
    // returned 500 while a plan was generating - which is the normal state, since the
    // frontend polls the status endpoint throughout generation.
    // ------------------------------------------------------------------

    @Test
    void statusConvertsStringStageFromRedisWithoutClassCastException() {
        UUID planId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .status(PlanStatus.GENERATING)
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Redis-dən qayıdan tipik JSON deserialize nəticəsi.
        when(valueOperations.get("plan:progress:" + planId)).thenReturn("SELECTING_PLACES");

        PlanStatusResponse status = queryService.getStatus(planId);

        assertThat(status.status()).isEqualTo(PlanStatus.GENERATING);
        assertThat(status.stage()).isEqualTo(GenerationStage.SELECTING_PLACES);
        assertThat(status.message()).isNotBlank();
    }

    @Test
    void statusToleratesMissingOrUnreadableStage() {
        UUID planId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .status(PlanStatus.GENERATING)
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Redis key-i yoxdur (TTL bitib və ya hələ yazılmayıb).
        when(valueOperations.get("plan:progress:" + planId)).thenReturn(null);
        assertThat(queryService.getStatus(planId).stage()).isNull();
        assertThat(queryService.getStatus(planId).message()).isNotBlank();
    }

    @Test
    void statusDegradesSafelyWhenRedisHoldsAnUnknownStageValue() {
        // Rollover case: a stage enum constant was renamed/removed in a deploy while an
        // old value is still cached. The status endpoint must degrade to a generic
        // "still preparing" message instead of returning 500 to a polling client.
        UUID planId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .status(PlanStatus.GENERATING)
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("plan:progress:" + planId)).thenReturn("SOME_REMOVED_STAGE");

        PlanStatusResponse status = queryService.getStatus(planId);

        assertThat(status.status()).isEqualTo(PlanStatus.GENERATING);
        assertThat(status.stage()).isNull();
        assertThat(status.message()).isEqualTo("Plan hazırlanır...");
    }

    @Test
    void statusIgnoresRedisForTerminalPlans() {
        // READY/FAILED plans short-circuit before touching Redis, so a missing or
        // corrupt progress key cannot affect the final message.
        UUID planId = UUID.randomUUID();
        TravelPlan ready = TravelPlan.builder().id(planId).status(PlanStatus.READY).build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(ready));

        PlanStatusResponse status = queryService.getStatus(planId);

        assertThat(status.status()).isEqualTo(PlanStatus.READY);
        assertThat(status.stage()).isNull();
        assertThat(status.message()).isEqualTo("Planın hazırdır");
    }

    @Test
    void statusReportsFailedPlansWithAFailureMessage() {
        UUID planId = UUID.randomUUID();
        TravelPlan failed = TravelPlan.builder().id(planId).status(PlanStatus.FAILED).build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(failed));

        PlanStatusResponse status = queryService.getStatus(planId);

        assertThat(status.status()).isEqualTo(PlanStatus.FAILED);
        assertThat(status.message()).isEqualTo("Plan hazırlanarkən xəta baş verdi");
    }
}
