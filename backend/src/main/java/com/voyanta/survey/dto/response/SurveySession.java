package com.voyanta.survey.dto.response;

import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;

import java.util.Set;

/**
 * Bu class DB entity DEYİL — Redis-də JSON kimi saxlanılır, TTL ilə (30 dəqiqə).
 * İstifadəçi "Planı hazırla" basana qədər müvəqqəti data daşıyır;
 * plan modulu bunu oxuyub TravelPlan-a çevirəcək.
 *
 * 7 sual: interests, companion(+familyDetails), budget, dates, hotelType, mealPreference, tripPurpose.
 */
public record SurveySession(
        String sessionId,
        Set<InterestType> interests,
        CompanionType companion,
        FamilyDetails familyDetails,
        Budget budget,
        TravelDates dates,
        HotelType hotelType,
        MealPreference mealPreference,
        Set<TripPurpose> tripPurpose,
        boolean completed
) {

    public static SurveySession empty(String sessionId) {
        return new SurveySession(
                sessionId, Set.of(), null, null, null, null,
                null, null, Set.of(), false
        );
    }
}