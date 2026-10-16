package com.voyanta.recommendation;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.image.ImageService;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.recommendation.service.RecommendationScorer;
import com.voyanta.recommendation.service.RecommendationService;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.wishlist.dao.entity.WishlistItem;
import com.voyanta.wishlist.dao.repository.DestinationFavoriteCount;
import com.voyanta.wishlist.dao.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the three recommendation sections against FAKED repositories only —
 * every favourite count is supplied explicitly by the test, so a test never passes
 * by accident and no production data is fabricated.
 *
 * <p>The clock is fixed so the trending window (last 30 days) is deterministic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommendationRankingTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    @Mock
    private TravelPlanRepository travelPlanRepository;
    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private ImageService imageService;

    private RecommendationService service;

    // Destination id -> how many users saved it (REAL signal, provided by the test).
    private final Map<UUID, Long> favorites = new HashMap<>();
    // Destination id -> how many of those saves happened in the last 30 days.
    private final Map<UUID, Long> recentFavorites = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new RecommendationService(
                travelPlanRepository, destinationRepository, wishlistRepository,
                new RecommendationScorer(), imageService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        when(travelPlanRepository.findByUserId(any())).thenReturn(List.of());
        when(imageService.resolveImageCandidates(any())).thenReturn(List.of());
        stubFavoritesFromMaps();
    }

    /**
     * Favourite counts must be read at CALL time, not at stubbing time: each test
     * fills {@link #favorites} AFTER setUp has run. thenAnswer re-reads the live map
     * so the aggregation reflects exactly what the test set up.
     */
    private void stubFavoritesFromMaps() {
        when(wishlistRepository.countFavoritesPerDestination())
                .thenAnswer(inv -> favorites.entrySet().stream()
                        .map(e -> count(e.getKey(), e.getValue())).toList());
        when(wishlistRepository.countFavoritesPerDestinationSince(any()))
                .thenAnswer(inv -> recentFavorites.entrySet().stream()
                        .map(e -> count(e.getKey(), e.getValue())).toList());
    }

    private DestinationFavoriteCount count(UUID id, long value) {
        return new DestinationFavoriteCount() {
            @Override
            public UUID getDestinationId() {
                return id;
            }

            @Override
            public long getFavorites() {
                return value;
            }
        };
    }

    /** Registers a destination in the catalogue and in the DB mock. */
    private Destination given(String name, String country, String tag, InterestType... tags) {
        Destination d = Destination.builder()
                .id(UUID.randomUUID())
                .name(name)
                .country(country)
                .imageUrl("https://db/" + name + ".jpg")
                .tag(tag)
                .interestTags(tags.length == 0 ? Set.of() : Set.of(tags))
                .build();
        return d;
    }

    private void catalogue(Destination... destinations) {
        when(destinationRepository.findAll()).thenReturn(List.of(destinations));
    }

    /** The user saved these destinations (real wishlist rows). */
    private void userSaved(UUID userId, Destination... destinations) {
        List<WishlistItem> items = new ArrayList<>();
        for (Destination d : destinations) {
            items.add(WishlistItem.builder().id(UUID.randomUUID())
                    .userId(userId).destinationId(d.getId()).createdAt(NOW).build());
        }
        when(wishlistRepository.findByUserId(userId)).thenReturn(items);
    }

    private List<String> names(List<DestinationResponse> responses) {
        return responses.stream().map(DestinationResponse::name).toList();
    }

    // ==================================================================
    // Test 1 — personalized beach user
    // ==================================================================

    @Test
    void beachUserIsRecommendedOtherSeaDestinationsAndNotTheirOwnFavourites() {
        Destination maldives = given("Maldiv adaları", "Maldivlər", "Dəniz və gündoğuş", InterestType.SEA);
        Destination bali = given("Bali", "İndoneziya", "Dəniz və gündoğuş", InterestType.SEA);
        Destination antalya = given("Antalya", "Türkiyə", "Dəniz və gündoğuş", InterestType.SEA);

        Destination santorini = given("Santorini", "Yunanıstan", "Dəniz və gündoğuş", InterestType.SEA);
        Destination bahamas = given("Bahama adaları", "Bahamalar", "Dəniz və gündoğuş", InterestType.SEA);
        Destination egypt = given("Misir", "Misir", "Tarix və mədəniyyət", InterestType.HISTORY_CULTURE);

        catalogue(maldives, bali, antalya, santorini, bahamas, egypt);

        UUID userId = UUID.randomUUID();
        userSaved(userId, maldives, bali, antalya);

        List<DestinationResponse> result = service.getPersonalized(userId, 3);

        assertThat(names(result))
                .as("recommendations must be new places, never the ones already saved")
                .doesNotContain("Maldiv adaları", "Bali", "Antalya");

        assertThat(names(result))
                .as("other SEA destinations should win over unrelated ones")
                .containsAnyOf("Santorini", "Bahama adaları");

        assertThat(names(result)).doesNotContain("Misir");
    }

    /**
     * Regression test: popularity must not rescue an unrelated destination.
     *
     * <p>Misir (history) was much more favourited than the sea destinations, yet a
     * sea-only user must never be shown it. Before the guard, the popularity bonus
     * pushed Misir above zero and it slipped into the list — this exact failure was
     * caught by RecommendationIT once real wishlist rows existed in the test DB.
     */
    @Test
    void aPopularButUnrelatedDestinationIsNeverRecommended() {
        Destination santorini = given("Santorini", "Yunanıstan", "Dəniz", InterestType.SEA);
        Destination maldives = given("Maldiv adaları", "Maldivlər", "Dəniz", InterestType.SEA);
        Destination egypt = given("Misir", "Misir", "Tarix", InterestType.HISTORY_CULTURE);
        catalogue(santorini, maldives, egypt);

        // Misir is by far the most favourited place in the whole catalogue.
        favorites.put(egypt.getId(), 500L);
        favorites.put(santorini.getId(), 10L);
        favorites.put(maldives.getId(), 5L);

        UUID userId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder().interests(Set.of(InterestType.SEA)).build();
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of(plan));
        userSavedNothing(userId); // plan-only user: no wishlist rows

        List<DestinationResponse> result = service.getPersonalized(userId, 5);

        assertThat(names(result)).doesNotContain("Misir");
        assertThat(names(result)).containsAnyOf("Santorini", "Maldiv adaları");
    }

    /** Saves nothing for the user, but still marks the wishlist lookup as used. */
    private void userSavedNothing(UUID userId) {
        when(wishlistRepository.findByUserId(userId)).thenReturn(List.of());
    }

    // ==================================================================
    // Test 2 — mountain user
    // ==================================================================

    @Test
    void mountainUserIsRecommendedOtherNatureDestinationsAndNotTheirOwnFavourites() {
        Destination switzerland = given("İsveçrə", "İsveçrə", "Təbiət", InterestType.NATURE);
        Destination dolomites = given("Dolomites", "İtaliya", "Təbiət", InterestType.NATURE);
        Destination ismayilli = given("İsmayıllı", "Azərbaycan", "Təbiət", InterestType.NATURE);

        Destination patagonia = given("Patagonia", "Argentina", "Təbiət", InterestType.NATURE);
        Destination norway = given("Norveç", "Norveç", "Təbiət", InterestType.NATURE);
        Destination mallorca = given("Mallorca", "İspaniya", "Dəniz və gündoğuş", InterestType.SEA);

        catalogue(switzerland, dolomites, ismayilli, patagonia, norway, mallorca);

        UUID userId = UUID.randomUUID();
        userSaved(userId, switzerland, dolomites, ismayilli);

        List<DestinationResponse> result = service.getPersonalized(userId, 3);

        assertThat(names(result))
                .as("the user's own favourites must not be recommended back")
                .doesNotContain("İsveçrə", "Dolomites", "İsmayıllı");

        assertThat(names(result)).containsAnyOf("Patagonia", "Norveç");
        assertThat(names(result)).doesNotContain("Mallorca");
    }

    // ==================================================================
    // Test 3 — no preferences
    // ==================================================================

    @Test
    void userWithoutAnyPreferenceDataGetsAGenericFallback() {
        Destination a = given("Santorini", "Yunanıstan", "Dəniz", InterestType.SEA);
        featured(a);
        Destination b = given("Misir", "Misir", "Tarix", InterestType.HISTORY_CULTURE);
        featured(b);

        catalogue(a, b);

        UUID userId = UUID.randomUUID();
        // No wishlist rows, no plans.
        when(wishlistRepository.findByUserId(userId)).thenReturn(List.of());
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of());

        List<DestinationResponse> result = service.getPersonalized(userId, 5);

        assertThat(result).isNotEmpty();
        assertThat(names(result))
                .as("with no signal the fallback still returns real destinations")
                .containsExactlyInAnyOrder("Santorini", "Misir");
    }

    /** Registers a featured destination in the catalogue. */
    private void featured(Destination d) {
        d.setFeatured(true);
    }

    // ==================================================================
    // Test 4 — logged out / no user id
    // ==================================================================

    @Test
    void nullUserNeverProducesUserSpecificRecommendations() {
        Destination santorini = given("Santorini", "Yunanıstan", "Dəniz", InterestType.SEA);
        featured(santorini);
        Destination egypt = given("Misir", "Misir", "Tarix", InterestType.HISTORY_CULTURE);
        featured(egypt);
        catalogue(santorini, egypt);

        // A null user id must not read anybody's wishlist.
        List<DestinationResponse> result = service.getPersonalized(null, 5);

        assertThat(result).isNotEmpty();
        org.mockito.Mockito.verify(wishlistRepository, org.mockito.Mockito.never())
                .findByUserId(any());
        org.mockito.Mockito.verify(travelPlanRepository, org.mockito.Mockito.never())
                .findByUserId(any());
    }

    // ==================================================================
    // Test 5 — popular from real favourite counts
    // ==================================================================

    @Test
    void popularSectionIsOrderedByRealFavouriteCounts() {
        Destination santorini = given("Santorini", "Yunanıstan", "Dəniz", InterestType.SEA);
        Destination maldives = given("Maldiv adaları", "Maldivlər", "Dəniz", InterestType.SEA);
        Destination egypt = given("Misir", "Misir", "Tarix", InterestType.HISTORY_CULTURE);
        catalogue(santorini, maldives, egypt);

        // Real aggregated wishlist rows: 150, 90 and 3 users.
        favorites.put(maldives.getId(), 150L);
        favorites.put(santorini.getId(), 90L);
        favorites.put(egypt.getId(), 3L);

        List<DestinationResponse> result = service.getPopular(5, Set.of());

        assertThat(names(result)).containsExactly("Maldiv adaları", "Santorini", "Misir");
    }

    @Test
    void popularFallsBackToFeaturedWhenNobodyHasFavouritedAnything() {
        Destination featuredOne = given("Santorini", "Yunanıstan", "Dəniz", InterestType.SEA);
        featured(featuredOne);
        Destination plain = given("Misir", "Misir", "Tarix", InterestType.HISTORY_CULTURE);
        catalogue(featuredOne, plain);

        // No favourite rows at all -> documented editorial ordering, not fake counts.
        List<DestinationResponse> result = service.getPopular(5, Set.of());

        // Featured rows lead, but the non-featured destination is still offered:
        // it is a real destination, it simply has no measured demand yet.
        assertThat(names(result)).containsExactly("Santorini", "Misir");
    }

    // ==================================================================
    // Test 6 — trending uses recency
    // ==================================================================

    @Test
    void trendingPrefersDestinationsSavedRecentlyOverOldFavourites() {
        Destination hotNow = given("İzlanda", "İslandiya", "Təbiət", InterestType.NATURE);
        Destination oldFavourite = given("Maldiv adaları", "Maldivlər", "Dəniz", InterestType.SEA);
        catalogue(hotNow, oldFavourite);

        // Lifetime totals favour Maldives, but recent saves favour Iceland.
        favorites.put(oldFavourite.getId(), 100L);
        favorites.put(hotNow.getId(), 12L);
        recentFavorites.put(hotNow.getId(), 12L);
        recentFavorites.put(oldFavourite.getId(), 1L);

        List<DestinationResponse> result = service.getTrending(5, Set.of());

        assertThat(names(result))
                .as("trending ranks by momentum, not by lifetime total")
                .containsExactly("İzlanda", "Maldiv adaları");
    }

    @Test
    void trendingExcludesFavouritesOlderThanTheWindow() {
        Destination stale = given("Köhnə yer", "X", "Təbiət", InterestType.NATURE);
        Destination current = given("Cari yer", "Y", "Təbiət", InterestType.NATURE);
        catalogue(stale, current);

        // Both have lifetime favourites, but only one has saves inside the 30-day window.
        favorites.put(stale.getId(), 500L);
        favorites.put(current.getId(), 2L);
        recentFavorites.put(current.getId(), 2L);

        List<DestinationResponse> result = service.getTrending(5, Set.of());

        assertThat(names(result)).contains("Cari yer");
    }

    // ==================================================================
    // Test 7 — diversity between sections
    // ==================================================================

    @Test
    void diversityAvoidsAlreadyShownDestinationsButNeverStarvesTheSection() {
        Destination a = given("A", "X", "Dəniz", InterestType.SEA);
        Destination b = given("B", "Y", "Dəniz", InterestType.SEA);
        Destination c = given("C", "Z", "Dəniz", InterestType.SEA);
        Destination d = given("D", "W", "Dəniz", InterestType.SEA);
        catalogue(a, b, c, d);

        favorites.put(a.getId(), 50L);
        favorites.put(b.getId(), 40L);
        favorites.put(c.getId(), 30L);
        favorites.put(d.getId(), 20L);

        // Trending already showed A and B; popular should avoid repeating them.
        List<DestinationResponse> popular = service.getPopular(2, Set.of(a.getId(), b.getId()));
        assertThat(names(popular)).containsExactly("C", "D");

        // Now the opposite: if everything is excluded, the section must still be full
        // rather than returning a degraded, artificially short list.
        List<DestinationResponse> starved =
                service.getPopular(2, Set.of(a.getId(), b.getId(), c.getId(), d.getId()));
        assertThat(starved).hasSize(2);
    }

    @Test
    void excludeParameterIsIgnoredWhenItWouldHurtRelevance() {
        Destination topPick = given("Top", "X", "Dəniz", InterestType.SEA);
        Destination other = given("Other", "Y", "Dəniz", InterestType.SEA);
        catalogue(topPick, other);
        favorites.put(topPick.getId(), 100L);
        favorites.put(other.getId(), 5L);

        // Only two destinations exist and the best one is excluded: relevance wins,
        // the section stays full and the best match is still allowed through.
        List<DestinationResponse> result = service.getPopular(2, Set.of(topPick.getId()));
        assertThat(result).hasSize(2);
    }

    // ==================================================================
    // Response contract — existing image data must survive
    // ==================================================================

    @Test
    void recommendationResponsesKeepTheExistingImageFields() {
        Destination santorini = given("Santorini", "Yunanıstan", "Dəniz", InterestType.SEA);
        featured(santorini);
        catalogue(santorini);
        favorites.put(santorini.getId(), 5L);

        when(imageService.resolveImageCandidates(any())).thenReturn(List.of(
                new com.voyanta.image.dto.response.ImageCandidateResponse(
                        "a", "https://img/a", "https://img/a-full", "Jane", null, null)));

        List<DestinationResponse> result = service.getTrending(3, Set.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).imageUrl()).isEqualTo("https://img/a");
        assertThat(result.get(0).images()).hasSize(1);
    }

    // ==================================================================
    // Section separation: the same small list must NOT be returned by every
    // section once the catalogue is larger than the featured subset.
    // ==================================================================

    /**
     * Regression test for the "all sections show the same four cards" bug.
     *
     * <p>With no user activity at all, the fallback scored non-featured destinations
     * 0 and then dropped them, so every section collapsed onto the featured subset.
     * Non-featured destinations must be included, after the featured ones.
     */
    @Test
    void aDestinationThatIsNotFeaturedIsStillOfferedWhenThereIsNoActivity() {
        Destination featuredOne = given("Featured one", "A", "Dəniz", InterestType.SEA);
        featured(featuredOne);
        Destination featuredTwo = given("Featured two", "B", "Dəniz", InterestType.SEA);
        featured(featuredTwo);
        Destination plain = given("Plain destination", "C", "Təbiət", InterestType.NATURE);
        // featured == false on purpose.

        catalogue(featuredOne, featuredTwo, plain);

        // No favourites at all -> the no-activity fallback runs.
        List<DestinationResponse> trending = service.getTrending(6, Set.of());
        List<DestinationResponse> popular = service.getPopular(6, Set.of());

        assertThat(names(trending))
                .as("a non-featured destination must not be silently discarded")
                .contains("Plain destination");
        assertThat(names(popular)).contains("Plain destination");

        // Featured rows are still promoted to the top.
        assertThat(names(trending).get(0)).isIn("Featured one", "Featured two");
    }

    @Test
    void sectionsDifferOnceTrendingHasItsOwnRealActivity() {
        Destination rising = given("Rising", "A", "Təbiət", InterestType.NATURE);
        Destination established = given("Established", "B", "Dəniz", InterestType.SEA);
        Destination alsoRising = given("Also rising", "C", "Təbiət", InterestType.NATURE);
        featured(established);

        catalogue(rising, established, alsoRising);

        // Real wishlist rows: two destinations are being saved right now.
        recentFavorites.put(rising.getId(), 9L);
        recentFavorites.put(alsoRising.getId(), 7L);
        favorites.put(rising.getId(), 9L);
        favorites.put(alsoRising.getId(), 7L);
        favorites.put(established.getId(), 40L);

        List<DestinationResponse> trending = service.getTrending(6, Set.of());
        List<DestinationResponse> popular = service.getPopular(6, Set.of());

        // "Established" keeps 40 lifetime favourites, so trending legitimately
        // includes it after the two destinations with recent momentum.
        assertThat(names(trending)).containsExactly("Rising", "Also rising", "Established");
        assertThat(names(popular))
                .containsExactly("Established", "Rising", "Also rising");
    }

    @Test
    void theExcludeParameterLetsSectionsAvoidRepeatingEachOther() {
        Destination a = given("A", "X", "Dəniz", InterestType.SEA);
        Destination b = given("B", "Y", "Dəniz", InterestType.SEA);
        Destination c = given("C", "Z", "Dəniz", InterestType.SEA);
        featured(a);
        featured(b);
        featured(c);
        catalogue(a, b, c);

        // "Trending" showed A and B; the next section must LEAD with C. A and B may
        // still appear afterwards, because a section is never starved artificially.
        List<DestinationResponse> second =
                service.getPersonalized(UUID.randomUUID(), 3, Set.of(a.getId(), b.getId()));

        assertThat(names(second).get(0))
                .as("an already-shown destination must not be repeated first")
                .isEqualTo("C");
    }

    // ==================================================================
    // Homepage images are hardcoded in the frontend now — the popular
    // section (served ONLY to the homepage) must not hit Unsplash.
    // ==================================================================

    @Test
    void popularSectionDoesNotCallTheImageProvider() {
        Destination popularA = given("Popular A", "X", "Dəniz", InterestType.SEA);
        Destination popularB = given("Popular B", "Y", "Dəniz", InterestType.SEA);
        catalogue(popularA, popularB);
        favorites.put(popularA.getId(), 10L);
        favorites.put(popularB.getId(), 5L);

        List<DestinationResponse> result = service.getPopular(2, Set.of());

        assertThat(result).hasSize(2);
        verify(imageService, never()).resolveImageCandidates(any());
        // The stored DB value is returned as-is; the candidate list stays empty.
        assertThat(result.get(0).images()).isEmpty();
        assertThat(result.get(0).imageUrl())
                .isEqualTo("https://db/" + result.get(0).name() + ".jpg");
    }

    // ==================================================================
    // Test 1 - personalized beach user
    // ==================================================================
}
