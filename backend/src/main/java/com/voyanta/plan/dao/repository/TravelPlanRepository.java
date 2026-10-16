package com.voyanta.plan.dao.repository;


import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the TravelPlan entity. It finds a plan by its survey
 * session id (used to avoid generating the same plan twice), counts plans by status for
 * the homepage stats, and lists all plans of one user.
 */
public interface TravelPlanRepository extends JpaRepository<TravelPlan, UUID> {
    Optional<TravelPlan> findByAnonymousSessionId(String anonymousSessionId);
    long countByStatus(PlanStatus status);
    List<TravelPlan> findByUserId(UUID userId);
}