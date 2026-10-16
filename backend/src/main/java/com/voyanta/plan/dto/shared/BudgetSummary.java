package com.voyanta.plan.dto.shared;

import java.math.BigDecimal;

// BigDecimal — pul məbləği üçün double/int yox, dəyirmilləşmə xətalarının qarşısını almaq üçün
/**
 * Shared DTO with the estimated trip costs split into accommodation, food, transport,
 * activities and total. It is saved as JSON on the TravelPlan entity and returned inside
 * PlanResponse.
 */
public record BudgetSummary(
        BigDecimal accommodation,
        BigDecimal food,
        BigDecimal transport,
        BigDecimal activities,
        BigDecimal total
) {}