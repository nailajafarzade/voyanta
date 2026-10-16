package com.voyanta.recommendation.service;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.image.DestinationImageQuery;
import com.voyanta.image.ImageService;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.wishlist.dao.repository.DestinationFavoriteCount;
import com.voyanta.wishlist.dao.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Destination recommendations: trending, popular and personalized ("Selected For You").
 *
 * <p><b>What real data this uses.</b> Everything below comes from rows users created:
 * <ul>
 *   <li>{@code wishlist.wishlist_items} — destinations users saved, with a timestamp.
 *       This is the popularity signal AND the strongest personalization signal.</li>
 *   <li>{@code plan.travel_plans.interests} — interests users actually picked.</li>
 *   <li>{@code destination.destinations.interest_tags} — what each destination is
 *       categorised as. This is what makes preference matching work.</li>
 * </ul>
 * There are no likes, views, clicks, bookings or ratings in this schema, and none
 * are simulated.
 *
 * <p><b>Seasonal trending is deliberately NOT implemented.</b> The destinations table
 * has no season, month, weather or geography column (verified against the entity and
 * all three Flyway migrations). Inventing an "October score" would be fabricating
 * data, so trending uses genuine recency instead: favourites added in the last
 * {@value #TRENDING_WINDOW_DAYS} days. When seasonal metadata is added to the
 * destination table later, only {@link RecommendationScorer#trendingScore} changes.
 */
@Service
public class RecommendationService {

    private static final int MAX_RESULTS = 6;

    /** Auto-created plan destinations ("Səyahət planı") never appear on the home page. */
    private static final String PLAN_TAG = "Səyahət planı";

    /** A wishlist row created within this many days counts as "current" interest. */
    static final int TRENDING_WINDOW_DAYS = 30;

    private final TravelPlanRepository travelPlanRepository;
    private final DestinationRepository destinationRepository;
    private final WishlistRepository wishlistRepository;
    private final RecommendationScorer scorer;
    private final ImageService imageService;
    private final Clock clock;

    /**
     * Single explicit constructor on purpose.
     *
     * <p>{@code @RequiredArgsConstructor} plus an extra convenience constructor
     * made Spring unable to pick one and the bean failed to start
     * ("No default constructor found"). One unambiguous constructor avoids that
     * and lets the Clock be injected, which is what makes the trending window
     * testable.
     */
    @Autowired
    public RecommendationService(TravelPlanRepository travelPlanRepository,
                                 DestinationRepository destinationRepository,
                                 WishlistRepository wishlistRepository,
                                 RecommendationScorer scorer,
                                 ImageService imageService,
                                 Clock clock) {
        this.travelPlanRepository = travelPlanRepository;
        this.destinationRepository = destinationRepository;
        this.wishlistRepository = wishlistRepository;
        this.scorer = scorer;
        this.imageService = imageService;
        this.clock = clock;
    }

    /** Production convenience constructor using the system clock. */
    public RecommendationService(TravelPlanRepository travelPlanRepository,
                                 DestinationRepository destinationRepository,
                                 WishlistRepository wishlistRepository,
                                 RecommendationScorer scorer,
                                 ImageService imageService) {
        this(travelPlanRepository, destinationRepository, wishlistRepository, scorer, imageService,
                Clock.systemUTC());
    }

    // ------------------------------------------------------------------
    // Selected For You (personalized)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DestinationResponse> getPersonalized(UUID userId, Integer limit) {
        return getPersonalized(userId, limit, Set.of());
    }

    /**
     * Personalized recommendations for a signed-in user.
     *
     * <p>Preferences come from the user's wishlist first (a deliberate action) and
     * from generated plan interests second. Destinations the user ALREADY saved are
     * excluded, so this section always surfaces something new.
     *
     * @param excludeIds destinations already used by another section on the same
     *                   page (diversity); overlap only happens if nothing else fits
     */
    @Transactional(readOnly = true)
    public List<DestinationResponse> getPersonalized(UUID userId, Integer limit, Set<UUID> excludeIds) {
        int effectiveLimit = limit != null && limit > 0 ? limit : MAX_RESULTS;
        List<Destination> candidates = destinationRepository.findAll();
        if (candidates.isEmpty()) {
            return List.of();
        }

        UserPreferenceProfile profile = buildProfile(userId, candidates);

        // No real preference data -> documented generic fallback, never fabricated.
        if (profile.isEmpty()) {
            return featuredRank(candidates, effectiveLimit, excludeIds);
        }

        Map<UUID, Long> favorites = totalFavorites();

        // "Already saved" must not be recommended back — show NEW destinations.
        Set<UUID> blocked = profile.seenIds();

        List<ScoredDestination> ranked = candidates.stream()
                .filter(d -> !blocked.contains(d.getId()))
                .map(d -> new ScoredDestination(d, scorer.personalizeScore(
                        d, profile.wishlistInterests(), profile.planInterests(),
                        profile.countries(), favorites)))
                .filter(s -> s.score() > 0)
                .sorted(byScoreThenName())
                .toList();

        if (ranked.isEmpty()) {
            // Everything the user saved is all we know about — fall back to the
            // widest pool rather than returning an empty section.
            return featuredRank(candidates, effectiveLimit, excludeIds);
        }

        return toResponses(selectWithDiversity(ranked, effectiveLimit, excludeIds));
    }

    // ------------------------------------------------------------------
    // Trending
    // ------------------------------------------------------------------

    /**
     * Trending destinations, driven by genuinely recent user activity.
     *
     * <p>Deliberately has NO relevance gate (unlike personalized): every row in the
     * destination table is a real place, so one with no favourites yet is still a
     * valid result — it just sorts last. Dropping it would collapse this section
     * onto whichever few destinations happen to have data, which is exactly the
     * "all sections show the same cards" problem.
     *
     * <p><b>Documented limitation:</b> the destination table carries no seasonal
     * metadata, so season (autumn/October) is NOT part of the score. Real recency is
     * used instead. See the class javadoc.
     */
    @Transactional(readOnly = true)
    public List<DestinationResponse> getTrending(Integer limit, Set<UUID> excludeIds) {
        int effectiveLimit = limit != null && limit > 0 ? limit : MAX_RESULTS;
        List<Destination> candidates = destinationRepository.findAll().stream()
                .filter(d -> !PLAN_TAG.equals(d.getTag()))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        Instant since = clock.instant().minus(Duration.ofDays(TRENDING_WINDOW_DAYS));
        Map<UUID, Long> recent = toCountMap(wishlistRepository.countFavoritesPerDestinationSince(since));
        Map<UUID, Long> total = totalFavorites();
        boolean noActivity = recent.isEmpty() && total.isEmpty();

        if (noActivity) {
            // A brand-new database has no user activity at all. Show the catalogue
            // with featured rows first rather than pretending this is a trend.
            return featuredRank(candidates, effectiveLimit, excludeIds);
        }

        List<ScoredDestination> ranked = candidates.stream()
                .map(d -> new ScoredDestination(d, scorer.trendingScore(d, recent, total)))
                .sorted(byScoreThenName())
                .toList();
        return toResponses(selectWithDiversity(ranked, effectiveLimit, excludeIds));
    }

    // ------------------------------------------------------------------
    // Popular
    // ------------------------------------------------------------------

    /**
     * Popular destinations ranked by REAL wishlist counts.
     *
     * <p>Ranked purely by real wishlist counts; the editorial {@code featured} flag is
     * only a tie-breaker. Destinations with zero favourites still appear — they are
     * real destinations, not irrelevant ones — they simply rank last.
     *
     * <p>Like trending, this has no relevance gate: see {@link #getTrending}.
     */
    @Transactional(readOnly = true)
    public List<DestinationResponse> getPopular(Integer limit, Set<UUID> excludeIds) {
        int effectiveLimit = limit != null && limit > 0 ? limit : MAX_RESULTS;
        List<Destination> candidates = destinationRepository.findAll().stream()
                .filter(d -> !PLAN_TAG.equals(d.getTag()))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> favorites = totalFavorites();

        List<ScoredDestination> ranked = candidates.stream()
                .map(d -> new ScoredDestination(d, scorer.popularScore(d, favorites)))
                .sorted(byScoreThenName())
                .toList();
        // Homepage-only section: the frontend hardcodes these images, so no Unsplash call.
        return toStoredImageResponses(selectWithDiversity(ranked, effectiveLimit, excludeIds));
    }

    // ------------------------------------------------------------------
    // Preference profile — derived only from stored behaviour
    // ------------------------------------------------------------------

    /**
     * Builds the profile from real rows: wishlist entries (strong) and plan
     * interests (weaker). Returns {@link UserPreferenceProfile#EMPTY} when the user
     * has genuinely produced nothing.
     */
    private UserPreferenceProfile buildProfile(UUID userId, List<Destination> allDestinations) {
        if (userId == null) {
            return UserPreferenceProfile.EMPTY;
        }

        Map<UUID, Destination> byId = allDestinations.stream()
                .collect(Collectors.toMap(Destination::getId, d -> d, (a, b) -> a));

        Map<InterestType, Integer> wishlistInterests = new EnumMap<>(InterestType.class);
        Map<String, Integer> countries = new HashMap<>();
        Set<UUID> seenIds = new LinkedHashSet<>();
        int sources = 0;

        for (var item : wishlistRepository.findByUserId(userId)) {
            UUID destinationId = item.getDestinationId();
            if (destinationId == null) {
                continue;
            }
            seenIds.add(destinationId);
            sources++;

            Destination destination = byId.get(destinationId);
            if (destination == null) {
                // The destination row was deleted; the id still counts as "seen"
                // so it is never recommended again.
                continue;
            }
            mergeInterests(wishlistInterests, destination.getInterestTags());
            String country = normalizeCountry(destination.getCountry());
            if (country != null) {
                countries.merge(country, 1, Integer::sum);
            }
        }

        Map<InterestType, Integer> planInterests = new EnumMap<>(InterestType.class);
        for (TravelPlan plan : travelPlanRepository.findByUserId(userId)) {
            if (plan.getInterests() != null && !plan.getInterests().isEmpty()) {
                mergeInterests(planInterests, plan.getInterests());
                sources++;
            }
        }

        if (sources == 0) {
            return UserPreferenceProfile.EMPTY;
        }
        return new UserPreferenceProfile(wishlistInterests, planInterests, countries, seenIds, sources);
    }

    private void mergeInterests(Map<InterestType, Integer> target, Set<InterestType> tags) {
        if (tags == null) {
            return;
        }
        for (InterestType tag : tags) {
            if (tag != null) {
                target.merge(tag, 1, Integer::sum);
            }
        }
    }

    // ------------------------------------------------------------------
    // Diversity between sections
    // ------------------------------------------------------------------

    /**
     * Picks up to {@code limit} destinations, preferring ones not already used by
     * another section — but NEVER at the cost of relevance.
     *
     * <p>Rule: take the best-ranked non-excluded destinations first. If that cannot
     * fill the section, top up from the excluded ones in their original rank order.
     * Overlap therefore only happens when there is genuinely nothing else to show.
     */
    private List<ScoredDestination> selectWithDiversity(List<ScoredDestination> ranked,
                                                        int limit,
                                                        Set<UUID> excludeIds) {
        if (excludeIds == null || excludeIds.isEmpty()) {
            return ranked.stream().limit(limit).toList();
        }

        List<ScoredDestination> fresh = ranked.stream()
                .filter(s -> !excludeIds.contains(s.destination().getId()))
                .limit(limit)
                .toList();
        if (fresh.size() >= limit) {
            return fresh;
        }

        Set<UUID> chosen = fresh.stream()
                .map(s -> s.destination().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ScoredDestination> result = new ArrayList<>(fresh);
        for (ScoredDestination candidate : ranked) {
            if (result.size() >= limit) {
                break;
            }
            if (chosen.add(candidate.destination().getId())) {
                result.add(candidate);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Fallback used when a section has no real signal for the user or the database.
     *
     * <p><b>Featured destinations are ranked FIRST, but they are not the only ones.</b>
     * The previous version scored them 1 and everything else 0, then filtered
     * {@code score > 0} - silently discarding every non-featured destination. That is
     * exactly why all homepage sections collapsed onto the same four cards.
     *
     * <p>Now every catalogue row is eligible and featured ones are promoted to the
     * top. Sections therefore differ as soon as the database holds more than the
     * featured subset, and nothing is invented to achieve it.
     */
    private List<DestinationResponse> featuredRank(List<Destination> candidates, int limit,
                                                  Set<UUID> excludeIds) {
        // No score filter here: 0 is a valid rank, not a reason to exclude.
        List<ScoredDestination> ranked = candidates.stream()
                .map(d -> new ScoredDestination(d, d.isFeatured() ? 1 : 0))
                .sorted(byScoreThenName())
                .toList();
        return toResponses(selectWithDiversity(ranked, limit, excludeIds));
    }

    private List<DestinationResponse> rank(List<Destination> candidates, int limit,
                                          Set<UUID> excludeIds, ToDoubleFunction<Destination> scoreFn) {
        List<ScoredDestination> ranked = candidates.stream()
                .map(d -> new ScoredDestination(d, scoreFn.applyAsDouble(d)))
                .sorted(byScoreThenName())
                .toList();
        return toResponses(selectWithDiversity(ranked, limit, excludeIds));
    }

    /** Highest score first, name as a stable tie-break so paging is deterministic. */
    private Comparator<ScoredDestination> byScoreThenName() {
        return Comparator.comparingDouble(ScoredDestination::score).reversed()
                .thenComparing(s -> s.destination().getName());
    }

    private List<DestinationResponse> toResponses(List<ScoredDestination> scored) {
        return scored.stream().map(s -> toResponse(s.destination())).collect(Collectors.toList());
    }

    /**
     * Maps ranked destinations WITHOUT touching the image provider (Unsplash).
     *
     * <p>Used by {@link #getPopular} only: that section is served exclusively to the
     * homepage, where the images are hardcoded in the frontend. The stored
     * {@code image_url} is returned as-is and the candidate list stays empty, which the
     * response serializer omits entirely (non_null inclusion).
     */
    private List<DestinationResponse> toStoredImageResponses(List<ScoredDestination> scored) {
        return scored.stream()
                .map(s -> new DestinationResponse(
                        s.destination().getId(),
                        s.destination().getName(),
                        s.destination().getCountry(),
                        s.destination().getImageUrl(),
                        s.destination().getTag(),
                        List.of()))
                .collect(Collectors.toList());
    }

    private Map<UUID, Long> totalFavorites() {
        return toCountMap(wishlistRepository.countFavoritesPerDestination());
    }

    private Map<UUID, Long> toCountMap(List<DestinationFavoriteCount> counts) {
        if (counts == null || counts.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> map = new HashMap<>();
        for (DestinationFavoriteCount count : counts) {
            if (count.getDestinationId() != null) {
                map.merge(count.getDestinationId(), count.getFavorites(), Long::sum);
            }
        }
        return map;
    }

    private String normalizeCountry(String country) {
        if (country == null) {
            return null;
        }
        String trimmed = country.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Ranking stays exactly as it was - only the image is resolved dynamically.
     * The database value is kept as a fallback so a card still renders when the
     * image provider is unavailable, and the candidate list is preserved so the
     * frontend can later show a different photo per card.
     */
    private DestinationResponse toResponse(Destination d) {
        List<ImageCandidateResponse> images = imageService.resolveImageCandidates(
                DestinationImageQuery.of(d.getName(), d.getCountry(), d.getTag(), d.getInterestTags()));
        String best = images.isEmpty() ? null : images.get(0).url();
        return new DestinationResponse(
                d.getId(), d.getName(), d.getCountry(),
                best != null ? best : d.getImageUrl(), d.getTag(), images
        );
    }

    private record ScoredDestination(Destination destination, double score) {
    }
}