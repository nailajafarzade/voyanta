package com.voyanta.ai.dto.response;

import java.math.BigDecimal;
import java.util.List;

// Diqqət: bu tip plan.dto.shared-dəki heç nəyi import etmir. ai modulu tam müstəqildir,
// AI-nin cavabını plan-ın öz tiplərinə çevirmək plan.service.PlanGenerationService-in işidir.
/**
 * Response DTO that mirrors the JSON schema the AI must return: the chosen destination,
 * the day-by-day itinerary and the budget breakdown. PlanGenerationService maps it into
 * the plan module's own entities.
 */
public record AiResponse(
        String destination,
        List<AiDayPlan> days,
        AiBudgetBreakdown budgetSummary
) {

    /** Nested DTO for one day of the trip: its day number and the activities planned for it. */
    public record AiDayPlan(
            int dayNumber,
            List<AiItineraryItem> items
    ) {}

    /** Nested DTO for a single activity in a day: its time, title, description and category. */
    public record AiItineraryItem(
            String time,
            String title,
            String description,
            String category
    ) {}

    /** Nested DTO with the estimated costs split into accommodation, food, transport, activities and total. */
    public record AiBudgetBreakdown(
            BigDecimal accommodation,
            BigDecimal food,
            BigDecimal transport,
            BigDecimal activities,
            BigDecimal total
    ) {}
}