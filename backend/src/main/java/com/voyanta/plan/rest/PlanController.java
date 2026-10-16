package com.voyanta.plan.rest;

import com.voyanta.common.dto.ApiResponse;
import com.voyanta.common.util.HashUtil;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dto.request.GeneratePlanRequest;
import com.voyanta.plan.dto.response.PlanResponse;
import com.voyanta.plan.dto.response.PlanStatusResponse;

import com.voyanta.plan.service.PlanClaimService;
import com.voyanta.plan.service.PlanGenerationService;
import com.voyanta.plan.service.PlanQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller that exposes the /api/plans endpoints: start generation, poll status,
 * read a plan and claim it after sign-in. It also builds the rate-limit key, using the
 * user id when signed in and a hashed client IP otherwise.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanGenerationService generationService;
    private final PlanQueryService queryService;
    private final PlanClaimService claimService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<UUID>> generate(
            @Valid @RequestBody GeneratePlanRequest request,
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest httpRequest
    ) {
        boolean isAuthenticated = userId != null;
        String rateLimitKey = isAuthenticated
                ? "user:" + userId
                : "ip:" + HashUtil.sha256(clientIp(httpRequest));

        TravelPlan plan = generationService.startGeneration(request.surveySessionId(), rateLimitKey, isAuthenticated);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(plan.getId()));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PlanStatusResponse>> status(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getStatus(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getPlan(id, userId)));
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<Void>> claim(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId
    ) {
        claimService.claim(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}