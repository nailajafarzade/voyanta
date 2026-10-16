package com.voyanta.survey.dto.request;

import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;

import java.util.Set;

// Hər PATCH sorğusunda yalnız həmin addımda cavablanan sual(lar)ın sahəsi dolu gəlir, qalanı null —
// service qatında yalnız null olmayan sahələr mövcud sessiyanın üzərinə yazılır.
public record UpdateSurveyRequest(
        Set<InterestType> interests,
        CompanionType companion,
        FamilyDetails familyDetails,
        Budget budget,
        TravelDates dates,
        HotelType hotelType,
        MealPreference mealPreference,
        Set<TripPurpose> tripPurpose
) {}