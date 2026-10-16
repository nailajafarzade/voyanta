package com.voyanta.recommendation.service;

import com.voyanta.survey.enums.InterestType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A user's travel preferences, DERIVED from behaviour that really happened.
 *
 * <p>Two sources are kept separate because they are not equally trustworthy:
 * <ul>
 *   <li>{@link #wishlistInterests()} — from destinations the user explicitly
 *       saved. A deliberate action, so it carries the higher weight.</li>
 *   <li>{@link #planInterests()} — from survey answers on generated plans. The
 *       user may have picked these without acting on them, so it carries less.</li>
 * </ul>
 *
 * <p>Nothing here is invented: every interest came from a stored
 * {@code interest_tags} value or a stored plan {@code interests} value, and every
 * country came from a destination the user actually saved.
 *
 * @param wishlistInterests interest -> occurrences, from saved destinations
 * @param planInterests     interest -> occurrences, from generated plans
 * @param countries         country  -> occurrences, from saved destinations
 * @param seenIds           destinations already saved or planned; excluded so
 *                          "Selected For You" always suggests something new
 * @param sourceCount       number of real data points behind this profile
 */
public record UserPreferenceProfile(
        Map<InterestType, Integer> wishlistInterests,
        Map<InterestType, Integer> planInterests,
        Map<String, Integer> countries,
        Set<UUID> seenIds,
        int sourceCount
) {

    public static final UserPreferenceProfile EMPTY =
            new UserPreferenceProfile(Map.of(), Map.of(), Map.of(), Set.of(), 0);

    public boolean isEmpty() {
        return sourceCount == 0
                || (wishlistInterests.isEmpty() && planInterests.isEmpty() && countries.isEmpty());
    }
}