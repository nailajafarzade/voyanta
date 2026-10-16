package com.voyanta.plan.service;

import com.voyanta.common.exception.ApiException;
import com.voyanta.common.exception.ResourceNotFoundException;

import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that attaches a plan created anonymously to a real user account after sign-in.
 * It refuses plans that already belong to someone else, and unlocks all itinerary days
 * once the plan is claimed.
 */
@Service
@RequiredArgsConstructor
public class PlanClaimService {

    private final TravelPlanRepository planRepository;
    private final PlanLockingService lockingService;

    @Transactional
    public void claim(UUID planId, UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Planı claim etmək üçün daxil olmalısan");
        }

        TravelPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan tapılmadı"));

        if (plan.getUserId() != null && !plan.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PLAN_ALREADY_CLAIMED", "Bu plan artıq başqa hesaba bağlıdır");
        }

        plan.setUserId(userId);
        lockingService.applyLocking(plan, true);   // artıq authenticated — bütün günlər açılır

        planRepository.save(plan);
    }
}