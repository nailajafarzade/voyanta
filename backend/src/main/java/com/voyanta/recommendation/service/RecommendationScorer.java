package com.voyanta.recommendation.service;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.survey.enums.InterestType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Simple, readable scoring rules for destination recommendations.
 *
 * <p>Deliberately NOT an ML model and NOT name-similarity matching. The signals
 * are exactly the ones the schema can support today:
 *
 * <pre>
 *   interest overlap      high weight — the primary signal
 *   country affinity      small bonus — a user who saved "Switzerland" often
 *                                   also likes nearby countries, but this must
 *                                   never outrank an interest match
 *   popularity (favourites) small bonus — a tie-breaker, not the ranking driver
 * </pre>
 *
 * <p>Why not name similarity: it would make "Selected For You" recommend copies
 * of what the user already saved. Matching on {@code interest_tags} instead lets a
 * Maldives+Bali user be shown other SEA destinations.
 */
@Component
public class RecommendationScorer {

    // Interest match: deliberately large. It is the only signal backed by data
    // the user actually produced.
    static final double WEIGHT_INTEREST = 3.0;

    // Plan interests are weaker than wishlist: the user may have answered a
    // survey without acting on it.
    static final double WEIGHT_PLAN_INTEREST = 1.5;

    static final double WEIGHT_COUNTRY = 0.75;

    // Trending: momentum must dominate history. 100 lifetime favourites (10.0)
    // must lose to 3 saves in the last 30 days (15.0), otherwise "trending" is
    // really just "was popular once".
    static final double WEIGHT_RECENT_FAVORITE = 5.0;
    static final double WEIGHT_LIFETIME_FAVORITE = 0.1;

    /** Editorial `featured` flag — a tie-breaker only, never the deciding signal. */
    static final double EDITORIAL_BONUS = 0.5;

    // Popularity contributes at most this much, so a popular-but-irrelevant
    // destination can never outrank a relevant one.
    static final double MAX_POPULARITY_BONUS = 1.5;
    private static final double FAVORITES_SATURATION = 20.0;

    /**
     * Scores one destination against a user's profile.
     *
     * <p><b>Popularity can never make an irrelevant destination relevant.</b>
     * Interest and country matches produce a {@code relevance} score; the popularity
     * bonus is added ONLY when that relevance is already positive. Without this
     * guard, a much-favourited destination the user has no connection to would
     * score above zero and slip into "Selected For You" — which is precisely the
     * unrelated-recommendation bug this section is supposed to avoid.
     *
     * @return 0 when the destination has no interest or country connection at all
     */
    public double personalizeScore(Destination destination,
                                   Map<InterestType, Integer> wishlistInterests,
                                   Map<InterestType, Integer> planInterests,
                                   Map<String, Integer> countries,
                                   Map<UUID, Long> favorites) {
        if (destination == null) {
            return 0;
        }

        // --- relevance: why this destination suits THIS user ---
        double relevance = 0;
        Set<InterestType> tags = destination.getInterestTags();
        if (tags != null && !tags.isEmpty()) {
            relevance += overlap(tags, wishlistInterests) * WEIGHT_INTEREST;
            relevance += overlap(tags, planInterests) * WEIGHT_PLAN_INTEREST;
        }

        String country = normalizeCountry(destination.getCountry());
        if (country != null) {
            Integer count = countries.get(country);
            if (count != null) {
                relevance += Math.min(count, 3) * WEIGHT_COUNTRY;
            }
        }

        if (relevance <= 0) {
            return 0;
        }

        // --- tie-breakers: only among destinations already known to be relevant ---
        return relevance + popularityBonus(destination.getId(), favorites);
    }

    /**
     * Popularity section: REAL favourites, with the editorial {@code featured}
     * flag and name only as deterministic tie-breakers so the list is stable.
     */
    public double popularScore(Destination destination, Map<UUID, Long> favorites) {
        return popularityBonus(destination.getId(), favorites)
                + (destination.isFeatured() ? EDITORIAL_BONUS : 0);
    }

    /**
     * Trending section: favourites added RECENTLY weigh much more than lifetime
     * favourites, because "trending" means current interest.
     *
     * <p>The recent weight must clearly dominate, otherwise a destination that was
     * hugely popular a year ago would outrank one being saved right now — the exact
     * opposite of trending. Hence 5.0 vs 0.1: 100 old favourites (10.0) still lose to
     * 3 recent ones (15.0).
     *
     * <p>Note: no seasonal factor is applied. The {@code destination.destinations}
     * table has no season/month/weather column, so any seasonal weight would be
     * invented. See {@link RecommendationService} for the documented fallback.
     */
    public double trendingScore(Destination destination,
                                Map<UUID, Long> recentFavorites,
                                Map<UUID, Long> totalFavorites) {
        double recent = favoritesOf(recentFavorites, destination.getId());
        double total = favoritesOf(totalFavorites, destination.getId());

        double score = recent * WEIGHT_RECENT_FAVORITE + total * WEIGHT_LIFETIME_FAVORITE;
        if (destination.isFeatured()) {
            score += EDITORIAL_BONUS;
        }
        return score;
    }

    /** log-saturating bonus so 200 favourites cannot dominate the ranking. */
    private double popularityBonus(UUID destinationId, Map<UUID, Long> favorites) {
        double count = favoritesOf(favorites, destinationId);
        if (count <= 0) {
            return 0;
        }
        return Math.min(count, FAVORITES_SATURATION) / FAVORITES_SATURATION * MAX_POPULARITY_BONUS;
    }

    private double favoritesOf(Map<UUID, Long> favorites, UUID id) {
        if (favorites == null || id == null) {
            return 0;
        }
        Long count = favorites.get(id);
        return count == null ? 0 : count;
    }

    private double overlap(Set<InterestType> destinationTags,
                           Map<InterestType, Integer> profile) {
        if (profile == null || profile.isEmpty()) {
            return 0;
        }
        double score = 0;
        for (InterestType tag : destinationTags) {
            Integer count = profile.get(tag);
            if (count != null) {
                // Saturating per interest: liking SEA twice should not double the
                // whole ranking, it should just confirm the preference.
                score += Math.min(count, 3) / 3.0;
            }
        }
        return score;
    }

    private String normalizeCountry(String country) {
        if (country == null) {
            return null;
        }
        String trimmed = country.trim().toLowerCase(java.util.Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }
}