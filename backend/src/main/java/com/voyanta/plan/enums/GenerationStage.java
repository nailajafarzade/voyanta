package com.voyanta.plan.enums;

/**
 * Enum with the finer steps of plan generation, from analyzing interests to DONE.
 * The current stage is kept in Redis while a plan is GENERATING, so the status endpoint
 * can show real progress text to the user.
 */
public enum GenerationStage {
    ANALYZING_INTERESTS, SELECTING_PLACES, BUILDING_ITINERARY, DONE
}