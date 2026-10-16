package com.voyanta.survey.enums;

/**
 * Enum with the budget levels offered in the survey, from ECONOMY to PREMIUM.
 * The special CUSTOM value means the user typed an exact amount instead, which is then
 * stored in the Budget DTO.
 */
public enum BudgetTier {
    ECONOMY,     // 200-400
    MID_RANGE,   // 400-800 / 800-1500
    COMFORT,
    PREMIUM,
    CUSTOM       // istifadəçi dəqiq məbləğ daxil edib
}