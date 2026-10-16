package com.voyanta.survey.dto.shared;

import com.voyanta.survey.enums.BudgetTier;

/**
 * Shared DTO for the budget answer in the survey. It carries the chosen budget tier, or
 * an exact amount when the user picked the CUSTOM tier.
 */
public record Budget(
        BudgetTier tier,
        Integer exactAmount   // yalnız tier = CUSTOM olanda dolu olur
) {}