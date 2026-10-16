package com.voyanta.homepage;

import com.fasterxml.jackson.databind.JsonNode;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.enums.PlanStatus;
import com.voyanta.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomepageStatsIT extends AbstractIntegrationTest {

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void statsAreLoadedFromDatabaseThenServedFromRedisCache() throws Exception {
        redisTemplate.delete("homepage:stats");
        long readyPlans = travelPlanRepository.countByStatus(PlanStatus.READY);

        JsonNode first = json(mockMvc.perform(get("/api/homepage/stats"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(first.path("plansCreated").asLong()).isEqualTo(readyPlans);
        assertThat(first.path("avgRating").decimalValue()).isEqualByComparingTo(new BigDecimal("4.9"));
        assertThat(redisTemplate.opsForValue().get("homepage:stats")).isNotNull();

        UUID planId = generatePlan(completeSurvey(createSurveySession().path("sessionId").asText()), null, uniqueIp());
        // Generation is asynchronous, so the plan must reach READY before it counts as "created".
        awaitPlanReady(planId);

        JsonNode cached = json(mockMvc.perform(get("/api/homepage/stats"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(cached.path("plansCreated").asLong()).isEqualTo(readyPlans);

        redisTemplate.delete("homepage:stats");
        JsonNode refreshed = json(mockMvc.perform(get("/api/homepage/stats"))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(refreshed.path("plansCreated").asLong()).isGreaterThanOrEqualTo(readyPlans + 1);
    }
}
