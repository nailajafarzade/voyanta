package com.voyanta.homepage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.homepage.dto.response.HomepageStatsResponse;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.plan.enums.PlanStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomepageStatsService {

    private static final String CACHE_KEY = "homepage:stats";

    private final TravelPlanRepository travelPlanRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final VoyantaProperties properties;
    private final ObjectMapper objectMapper;

    public HomepageStatsResponse getStats() {
        HomepageStatsResponse cached = readFromCache();
        if (cached != null) {
            return cached;
        }

        long plansCreated = travelPlanRepository.countByStatus(PlanStatus.READY);
        HomepageStatsResponse stats =
                new HomepageStatsResponse(plansCreated, properties.getHomepage().getAvgRating());

        writeToCache(stats);
        return stats;
    }

    private HomepageStatsResponse readFromCache() {
        try {
            Object value = redisTemplate.opsForValue().get(CACHE_KEY);
            if (value == null) {
                return null;
            }
            // LinkedHashMap və ya real obyekt, hər ikisini düzgün çevirir
            return objectMapper.convertValue(value, HomepageStatsResponse.class);
        } catch (Exception e) {
            log.warn("Homepage stats cache oxunmadı, DB-dən götürüləcək", e);
            return null;
        }
    }

    private void writeToCache(HomepageStatsResponse stats) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, stats, properties.getHomepage().getStatsCacheTtl());
        } catch (Exception e) {
            log.warn("Homepage stats cache-ə yazılmadı", e);
        }
    }
}