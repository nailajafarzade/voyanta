package com.voyanta.ai;

import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;

/**
 * Interface that the plan module uses to ask the AI for a travel itinerary.
 * It is the only public entry point of the ai module, so PlanGenerationService does not
 * depend on which AI provider is used behind it.
 */
public interface AiPlanGenerator {
    AiResponse generate(AiRequest request);
}