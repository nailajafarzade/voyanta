package com.voyanta.plan.service;

import com.voyanta.plan.dao.entity.ItineraryDay;
import com.voyanta.plan.dao.entity.TravelPlan;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that decides which itinerary days a user may see. Signed-in users get every
 * day unlocked, while anonymous users only keep the first two days open and the rest
 * locked.
 */
@Service
public class PlanLockingService {

    private static final int FREE_VISIBLE_DAYS = 2;

    public void applyLocking(TravelPlan plan, boolean isAuthenticated) {
        if (isAuthenticated) {
            plan.getDays().forEach(day -> day.setLocked(false));
            return;
        }

        List<ItineraryDay> days = plan.getDays();
        for (int i = 0; i < days.size(); i++) {
            days.get(i).setLocked(i >= FREE_VISIBLE_DAYS);
        }
    }
}