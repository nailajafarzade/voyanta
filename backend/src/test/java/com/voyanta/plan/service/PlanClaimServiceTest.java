package com.voyanta.plan.service;

import com.voyanta.common.exception.ApiException;
import com.voyanta.common.exception.ResourceNotFoundException;
import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanClaimServiceTest {

    @Mock
    private TravelPlanRepository planRepository;
    @Mock
    private PlanLockingService lockingService;
    @InjectMocks
    private PlanClaimService claimService;

    @Test
    void claimRequiresAuthentication() {
        assertThatThrownBy(() -> claimService.claim(UUID.randomUUID(), null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo("AUTH_REQUIRED");
    }

    @Test
    void claimAttachesUserAndUnlocksDays() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder()
                .id(planId)
                .days(new ArrayList<>(List.of(ItineraryDay.builder().dayNumber(1).locked(true).build())))
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        claimService.claim(planId, userId);

        assertThat(plan.getUserId()).isEqualTo(userId);
        verify(lockingService).applyLocking(plan, true);
        verify(planRepository).save(plan);
    }

    @Test
    void claimOfSomeoneElsesPlanIsForbidden() {
        UUID planId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder().id(planId).userId(UUID.randomUUID()).build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> claimService.claim(planId, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo("PLAN_ALREADY_CLAIMED");
    }

    @Test
    void missingPlanIsNotFound() {
        UUID planId = UUID.randomUUID();
        when(planRepository.findById(planId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> claimService.claim(planId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
