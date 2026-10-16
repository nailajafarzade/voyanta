package com.voyanta.plan.dto.response;

import com.voyanta.plan.enums.GenerationStage;
import com.voyanta.plan.enums.PlanStatus;

/**
 * Response DTO used while the frontend polls the plan status endpoint. It returns the
 * plan status, the current generation stage and a ready-made message that the UI can
 * show as-is.
 */
public record PlanStatusResponse(
        PlanStatus status,
        GenerationStage stage,   // yalnız status = GENERATING olanda dolu
        String message           // frontend-in birbaşa göstərdiyi hazır mətn
) {}