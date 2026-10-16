package com.voyanta.plan.enums;

/**
 * Enum with the overall state of a travel plan: GENERATING while the AI is working,
 * READY when the itinerary is saved, and FAILED if generation broke. It is stored on the
 * TravelPlan entity and returned to the client while polling.
 */
public enum PlanStatus {
    GENERATING, READY, FAILED
}