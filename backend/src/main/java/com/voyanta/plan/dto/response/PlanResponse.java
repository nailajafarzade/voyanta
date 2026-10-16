package com.voyanta.plan.dto.response;

import com.voyanta.image.dto.response.ImageCandidateResponse;
import com.voyanta.plan.dto.shared.BudgetSummary;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.survey.enums.CompanionType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a single travel plan. It returns the destination, dates, companion,
 * budget summary, status and the list of itinerary days, where locked days come back
 * without their activities.
 *
 * imageUrl is resolved by ImageService from the destination name the AI produced, so it
 * works for any destination (Greenland, Svalbard, ...) without a predefined list. It is
 * null when no image could be resolved - the rest of the plan is returned either way.
 *
 * <p>images carries ALL relevant photo candidates for the same destination, so the
 * frontend can offer a choice / a full-screen view without another backend call.
 * The AI plan generation itself is untouched - only the image layer feeding it changed.
 */
public record PlanResponse(
        UUID id,
        String destination,
        UUID destinationId,
        String imageUrl,
        List<ImageCandidateResponse> images,
        LocalDate startDate,
        LocalDate endDate,
        CompanionType companion,
        BudgetSummary budgetSummary,
        PlanStatus status,
        List<ItineraryDayResponse> days
) {}