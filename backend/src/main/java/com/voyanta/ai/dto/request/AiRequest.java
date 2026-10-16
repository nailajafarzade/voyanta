package com.voyanta.ai.dto.request;

import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;

import java.util.Set;

public record AiRequest(
        Set<InterestType> interests,
        CompanionType companion,
        FamilyDetails familyDetails,
        Budget budget,
        TravelDates dates,
        HotelType hotelType,
        MealPreference mealPreference,
        Set<TripPurpose> tripPurpose
) {}