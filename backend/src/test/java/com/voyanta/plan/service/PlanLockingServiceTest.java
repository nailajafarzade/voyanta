package com.voyanta.plan.service;

import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanLockingServiceTest {

    private final PlanLockingService lockingService = new PlanLockingService();

    @Test
    void anonymousUsersKeepTheFirstTwoDaysOpen() {
        TravelPlan plan = planWithDays(4);
        lockingService.applyLocking(plan, false);
        assertThat(plan.getDays().get(0).isLocked()).isFalse();
        assertThat(plan.getDays().get(1).isLocked()).isFalse();
        assertThat(plan.getDays().get(2).isLocked()).isTrue();
        assertThat(plan.getDays().get(3).isLocked()).isTrue();
    }

    @Test
    void authenticatedUsersSeeEveryDay() {
        TravelPlan plan = planWithDays(3);
        plan.getDays().forEach(day -> day.setLocked(true));
        lockingService.applyLocking(plan, true);
        assertThat(plan.getDays()).allMatch(day -> !day.isLocked());
    }

    private static TravelPlan planWithDays(int count) {
        TravelPlan plan = new TravelPlan();
        List<ItineraryDay> days = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            days.add(ItineraryDay.builder().dayNumber(i).locked(false).build());
        }
        plan.setDays(days);
        return plan;
    }
}
