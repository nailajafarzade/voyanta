package com.voyanta.plan.dto.response;

import com.voyanta.plan.dto.shared.ItineraryItem;

import java.util.List;

/**
 * Response DTO for one day inside a PlanResponse. It always tells the day number and
 * whether the day is locked, but the activities are only filled in when the user is
 * allowed to see them.
 */
public record ItineraryDayResponse(
        int dayNumber,
        boolean locked,
        List<ItineraryItem> items   // locked = true olanda null qaytarılır
) {}