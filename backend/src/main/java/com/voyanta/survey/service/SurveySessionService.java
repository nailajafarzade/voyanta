package com.voyanta.survey.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ResourceNotFoundException;
import com.voyanta.survey.dto.request.UpdateSurveyRequest;
import com.voyanta.survey.dto.response.SurveySession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurveySessionService {

    private static final String KEY_PREFIX = "survey:session:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final VoyantaProperties properties;
    private final ObjectMapper objectMapper;

    public SurveySession create() {
        String sessionId = UUID.randomUUID().toString();
        SurveySession session = SurveySession.empty(sessionId);
        save(session);
        return session;
    }

    public SurveySession get(String sessionId) {
        Object value = redisTemplate.opsForValue().get(key(sessionId));
        if (value == null) {
            throw new ResourceNotFoundException("Sorğu sessiyası tapılmadı və ya vaxtı bitib");
        }
        // Redis-dən LinkedHashMap də gələ bilər, real obyekt də: convertValue hər ikisini düzgün çevirir
        return objectMapper.convertValue(value, SurveySession.class);
    }

    public SurveySession update(String sessionId, UpdateSurveyRequest request) {
        SurveySession current = get(sessionId);

        SurveySession updated = new SurveySession(
                current.sessionId(),
                request.interests() != null ? request.interests() : current.interests(),
                request.companion() != null ? request.companion() : current.companion(),
                request.familyDetails() != null ? request.familyDetails() : current.familyDetails(),
                request.budget() != null ? request.budget() : current.budget(),
                request.dates() != null ? request.dates() : current.dates(),
                request.hotelType() != null ? request.hotelType() : current.hotelType(),
                request.mealPreference() != null ? request.mealPreference() : current.mealPreference(),
                request.tripPurpose() != null ? request.tripPurpose() : current.tripPurpose(),
                current.completed()
        );

        save(updated);
        return updated;
    }

    public void markCompleted(String sessionId) {
        SurveySession current = get(sessionId);
        save(new SurveySession(
                current.sessionId(), current.interests(), current.companion(),
                current.familyDetails(), current.budget(), current.dates(),
                current.hotelType(), current.mealPreference(), current.tripPurpose(),
                true
        ));
    }

    private void save(SurveySession session) {
        Duration ttl = properties.getSurvey().getSessionTtl();
        redisTemplate.opsForValue().set(key(session.sessionId()), session, ttl);
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}