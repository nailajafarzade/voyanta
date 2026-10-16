package com.voyanta.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import com.voyanta.survey.enums.InterestType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the image resolution against a fake Unsplash (MockWebServer), never the real API.
 *
 * The behaviours that matter in production are all here: the query is derived from the
 * destination data (including the Azerbaijani names that are actually stored in the DB),
 * results are ranked by relevance rather than taken blindly, and NOTHING in the failure
 * path is allowed to throw.
 */
class UnsplashImageServiceTest {

    private MockWebServer server;
    private VoyantaProperties properties;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private DestinationImageQueryBuilder queryBuilder;
    private UnsplashResultRanker ranker;
    private UnsplashImageService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        properties = new VoyantaProperties();
        properties.getUnsplash().setAccessKey("test-access-key");
        properties.getUnsplash().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getUnsplash().setSearchPath("/search/photos");
        properties.getUnsplash().setOrientation("landscape");
        properties.getUnsplash().setContentFilter("high");
        properties.getUnsplash().setPerPage(10);
        properties.getUnsplash().setTimeout(Duration.ofSeconds(3));
        properties.getUnsplash().setCacheTtl(Duration.ofDays(30));
        properties.getUnsplash().setNegativeCacheTtl(Duration.ofHours(6));

        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        queryBuilder = new DestinationImageQueryBuilder();
        ranker = new UnsplashResultRanker();

        service = new UnsplashImageService(
                WebClient.builder(), properties, redisTemplate, new ObjectMapper(),
                queryBuilder, ranker
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ------------------------------------------------------------------
    // Query construction: derived from the destination, never hardcoded.
    // These are the queries that decide whether a user sees the pyramids.
    // ------------------------------------------------------------------

    @Test
    void egyptIsSearchedByItsLandmarkRatherThanTheGenericCountryWord() throws Exception {
        server.enqueue(results(photo("egypt-pyramid", "Pyramids of Giza in Egypt", "pyramid desert egypt")));

        service.resolveImageUrl(DestinationImageQuery.of("Misir", "Misir", "Tarix və mədəniyyət",
                Set.of(InterestType.HISTORY_CULTURE)));

        assertThat(server.takeRequest().getPath())
                .contains("query=Giza%20pyramids%20Egypt");
    }

    @Test
    void aNatureDestinationSearchesForItsLandscapeNotForBuildings() throws Exception {
        server.enqueue(results(photo("mountains", "Mountain peaks in Ismayilli", "mountains nature azerbaijan")));

        service.resolveImageUrl(DestinationImageQuery.of("İsmayıllı", "Azərbaycan", "Təbiət",
                Set.of(InterestType.NATURE)));

        String path = server.takeRequest().getPath();
        assertThat(path).contains("query=Ismayilli%20Azerbaijan%20mountains%20nature");
        assertThat(path).doesNotContain("city");
        assertThat(path).doesNotContain("buildings");
    }

    @Test
    void aSeaDestinationSearchesForBeachAndIsland() throws Exception {
        server.enqueue(results(photo("maldives", "Overwater villas Maldives", "maldives island beach")));

        service.resolveImageUrl(DestinationImageQuery.of("Maldiv adaları", "Maldivlər",
                "Dəniz və gündoğuş", Set.of(InterestType.SEA)));

        assertThat(server.takeRequest().getPath())
                .contains("query=Maldives%20tropical%20island%20beach");
    }

    @Test
    void aKnownCapitalIsSearchedByItsIconicLandmark() throws Exception {
        server.enqueue(results(photo("paris", "Eiffel Tower in Paris", "eiffel tower paris france")));

        service.resolveImageUrl(DestinationImageQuery.of("Paris", "Fransa", null, Set.of()));

        assertThat(server.takeRequest().getPath()).contains("query=Eiffel%20Tower%20Paris%20France");
    }

    @Test
    void theGenericTravelWordsAreNeverUsed() {
        for (String destination : new String[]{"Misir", "İsmayıllı", "Maldiv adaları", "Greenland"}) {
            assertThat(queryBuilder.build(DestinationImageQuery.of(destination, null, null, Set.of())))
                    .allSatisfy(q -> assertThat(q.toLowerCase()).doesNotContain("travel"));
        }
    }

    @Test
    void uncommonDestinationsStillGetARepresentativeSearch() {
        // Greenland üçün coğrafi xüsusiyyət sözü ("Arctic landscape") əlavə olunur,
        // çünki yalnız "Greenland" axtarışı quru/şəhər şəkli verə bilər.
        assertThat(queryBuilder.build(DestinationImageQuery.of("Greenland", null, null, Set.of())))
                .isNotEmpty()
                .allSatisfy(q -> assertThat(q.toLowerCase()).contains("greenland"))
                .anySatisfy(q -> assertThat(q.toLowerCase()).contains("arctic"));

        assertThat(queryBuilder.build(DestinationImageQuery.of("Svalbard", null, null, Set.of())))
                .isNotEmpty()
                .anySatisfy(q -> assertThat(q.toLowerCase()).contains("arctic"));
    }

    @Test
    void anUnknownDestinationStillProducesAUsableQuery() {
        // Yeni, lüğətdə olmayan ad: heç bir kod dəyişmədən işləməlidir.
        assertThat(queryBuilder.build(DestinationImageQuery.of("Naxçıvan Şəhəri", "Azərbaycan", null, Set.of())))
                .isNotEmpty();
    }

    @Test
    void theAccessKeyIsSentAsAClientIdHeaderAndNeverAppearsInTheQuery() throws Exception {
        server.enqueue(results(photo("dolomites", "Dolomites in Italy", "dolomites italy mountains")));

        service.resolveImageUrl(DestinationImageQuery.of("Dolomites", "Italy", null, Set.of()));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Client-ID test-access-key");
        assertThat(request.getPath()).doesNotContain("test-access-key");
    }

    @Test
    void aCountryMakesTheFirstQueryMoreSpecific() throws Exception {
        server.enqueue(results(photo("dolomites", "Dolomites in Italy", "dolomites italy mountains")));

        service.resolveImageUrl(DestinationImageQuery.of("Dolomites", "Italy", null, Set.of()));

        // Kateqoriya verilmədiyi üçün burada ümumi söz əlavə olunmur; sadəcə
        // ad + ölkə birləşdirilir — bu, "Dolomites Italy travel" ilə fərqlidir.
        assertThat(server.takeRequest().getPath()).contains("query=Dolomites%20Italy");
    }

    // ------------------------------------------------------------------
    // Result selection: relevance first, never "whatever comes first".
    // ------------------------------------------------------------------

    @Test
    void theMostRelevantResultIsChosenEvenWhenItIsNotTheFirstOne() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":3,\"results\":["
                        + photo("generic-beach", "A tropical beach with palm trees", "beach sand ocean")
                        + "," + photo("wrong", "An office building interior", "office desk city")
                        + "," + photo("pyramids", "The Great Pyramid of Giza in Egypt", "pyramid giza egypt")
                        + "]}"));

        String imageUrl = service.resolveImageUrl(DestinationImageQuery.of("Egypt", "Egypt", null, Set.of()));

        assertThat(imageUrl).isEqualTo("https://images.unsplash.com/pyramids");
    }

    @Test
    void aResultWithNoRelationToTheDestinationIsRejectedRatherThanShown() throws Exception {
        // Bütün nəticələr aləaqəsizdir -> heç biri qəbul edilməmir, növbəti sorğuya keçilir.
        server.enqueue(results(photo("beach", "A tropical beach with palm trees", "beach sand ocean")));
        server.enqueue(results(photo("pyramids", "The Great Pyramid of Giza in Egypt", "pyramid giza egypt")));

        String imageUrl = service.resolveImageUrl(DestinationImageQuery.of("Egypt", "Egypt", null, Set.of()));

        assertThat(server.getRequestCount()).isEqualTo(2);
        assertThat(imageUrl).isEqualTo("https://images.unsplash.com/pyramids");
    }

    @Test
    void skipsResultsWithoutAUsableUrl() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":2,\"results\":["
                        + "{\"urls\":{}},"
                        + "{\"urls\":{\"regular\":null,\"small\":\"\"}},"
                        + photo("bhutan", "Monastery in Bhutan", "bhutan monastery himalaya")
                        + "]}"));

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Bhutan", null, null, Set.of())))
                .isEqualTo("https://images.unsplash.com/bhutan");
    }

    // ------------------------------------------------------------------
    // Fallback chain: broader query, then bare destination name, then give up.
    // ------------------------------------------------------------------

    @Test
    void fallsBackToBroaderQueriesUntilSomethingIsFound() throws Exception {
        server.enqueue(emptyResults());
        server.enqueue(results(photo("faroe", "Cliffs of the Faroe Islands", "faroe islands cliffs")));

        String imageUrl = service.resolveImageUrl(DestinationImageQuery.of("Faroe Islands", null, null, Set.of()));

        assertThat(imageUrl).isEqualTo("https://images.unsplash.com/faroe");
        // Əvvəlki coğrafi xüsusiyyət sorğusu boş qaytarır, ikinci (yalnız ad) uyğun şəkil verir.
        assertThat(server.takeRequest().getPath()).contains("query=Faroe%20Islands%20cliff%20coastline");
        assertThat(server.takeRequest().getPath()).contains("query=Faroe%20Islands&");
    }

    @Test
    void returnsNullAfterEveryQueryIsExhaustedInsteadOfThrowing() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(emptyResults());
        }

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Svalbard", null, null, Set.of()))).isNull();
    }

    // ------------------------------------------------------------------
    // Multi-candidate responses: the frontend needs several relevant photos
    // so two cards showing the same destination do not look identical.
    // ------------------------------------------------------------------

    @Test
    void severalRelevantImagesAreReturnedSoCardsCanDiffer() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":3,\"results\":["
                        + photo("p1", "Pyramids of Giza in Egypt", "pyramid giza egypt")
                        + "," + photo("p2", "The Nile river at Luxor in Egypt", "nile river egypt")
                        + "," + photo("p3", "Temple of Karnak in Egypt", "temple egypt")
                        + "]}"));

        List<ImageCandidateResponse> images =
                service.resolveImageCandidates(DestinationImageQuery.of("Egypt", "Egypt", null, Set.of()));

        assertThat(images).hasSize(3);
        // Relevance order: the most destination-relevant photo comes first.
        assertThat(images.get(0).url()).isEqualTo("https://images.unsplash.com/p1");
        assertThat(images).extracting(ImageCandidateResponse::url)
                .doesNotHaveDuplicates();
    }

    @Test
    void irrelevantPhotosAreExcludedFromTheCandidateList() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":3,\"results\":["
                        + photo("beach", "A tropical beach with palm trees", "beach sand ocean")
                        + "," + photo("office", "An office desk interior", "office city desk")
                        + "," + photo("real", "The Great Pyramid of Giza in Egypt", "pyramid giza egypt")
                        + "]}"));

        List<ImageCandidateResponse> images =
                service.resolveImageCandidates(DestinationImageQuery.of("Egypt", "Egypt", null, Set.of()));

        assertThat(images).extracting(ImageCandidateResponse::url)
                .containsExactly("https://images.unsplash.com/real");
    }

    @Test
    void eachCandidateCarriesTheMetadataTheFrontendNeeds() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":1,\"results\":[{"
                        + "\"id\":\"abc123\",\"width\":2400,\"height\":1600,"
                        + "\"alt_description\":\"Pyramids of Giza in Egypt\","
                        + "\"urls\":{\"raw\":\"https://images.unsplash.com/raw-abc\","
                        + "\"full\":\"https://images.unsplash.com/full-abc\","
                        + "\"regular\":\"https://images.unsplash.com/photo-abc\"},"
                        + "\"user\":{\"name\":\"Jane Doe\",\"links\":{\"html\":\"https://unsplash.com/@jane\"}},"
                        + "\"links\":{\"html\":\"https://unsplash.com/photos/abc123\"},"
                        + "\"tags\":[{\"title\":\"pyramid\"}]"
                        + "}]}"));

        ImageCandidateResponse candidate = service.resolveImageCandidates(
                DestinationImageQuery.of("Egypt", "Egypt", null, Set.of())).get(0);

        assertThat(candidate.id()).isEqualTo("abc123");
        assertThat(candidate.url()).isEqualTo("https://images.unsplash.com/photo-abc");
        assertThat(candidate.fullUrl()).isEqualTo("https://images.unsplash.com/full-abc");
        assertThat(candidate.photographer()).isEqualTo("Jane Doe");
        assertThat(candidate.photographerUrl()).isEqualTo("https://unsplash.com/@jane");
        assertThat(candidate.unsplashUrl()).isEqualTo("https://unsplash.com/photos/abc123");
    }

    @Test
    void fullUrlFallsBackToTheCardUrlWhenNoLargerSizeExists() throws Exception {
        server.enqueue(results(photo("onlyregular", "Ice in Greenland", "greenland ice")));

        ImageCandidateResponse candidate = service.resolveImageCandidates(
                DestinationImageQuery.of("Greenland", null, null, Set.of())).get(0);

        assertThat(candidate.fullUrl()).isEqualTo(candidate.url());
    }

    @Test
    void duplicateUrlsAreCollapsedInTheCandidateList() throws Exception {
        // Eyni foto iki dəfə qaytarılır — siyahıda bir dəfə görünməlidir.
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":2,\"results\":["
                        + photo("dup", "Pyramids of Giza in Egypt", "pyramid giza egypt")
                        + "," + photo("dup", "Pyramids of Giza in Egypt", "pyramid giza egypt")
                        + "]}"));

        assertThat(service.resolveImageCandidates(DestinationImageQuery.of("Egypt", "Egypt", null, Set.of())))
                .extracting(ImageCandidateResponse::url)
                .containsExactly("https://images.unsplash.com/dup");
    }

    // ------------------------------------------------------------------
    // Redis caching: the same destination must not hit Unsplash twice.
    // ------------------------------------------------------------------

    @Test
    void resolvesOnceAndServesSubsequentCallsFromRedis() throws Exception {
        server.enqueue(results(photo("greenland", "Icebergs in Greenland", "greenland ice arctic")));
        when(valueOperations.get("image:v2:greenland"))
                .thenReturn(null, cachedJson("greenland"));

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Greenland", null, null, Set.of())))
                .isEqualTo("https://images.unsplash.com/greenland");
        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Greenland", null, null, Set.of())))
                .as("second call is answered from Redis without a new Unsplash request")
                .isEqualTo("https://images.unsplash.com/greenland");

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /**
     * Keş artıq TƏK URL deyil, bütün namizəd siyahısıdır — beləcə eyni
     * destinationsa ikinci dəfə göstərildikdə təkrar yoxdur.
     */
    @Test
    void theCacheStoresTheWholeCandidateListNotASingleUrl() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":2,\"results\":["
                        + photo("g1", "Pyramids of Giza in Egypt", "pyramid giza egypt")
                        + "," + photo("g2", "The Nile river in Egypt", "nile river egypt")
                        + "]}"));

        List<ImageCandidateResponse> images =
                service.resolveImageCandidates(DestinationImageQuery.of("Egypt", "Egypt", null, Set.of()));

        assertThat(images).hasSize(2);

        ArgumentCaptor<String> cached = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(ArgumentMatchers.eq("image:v2:egypt"), cached.capture(),
                ArgumentMatchers.eq(Duration.ofDays(30)));

        // Keçən dəyər JSON massivdir, tək URL deyil: içində BÜTÜN namizədlər var.
        assertThat(cached.getValue())
                .startsWith("[")
                .contains("https://images.unsplash.com/g1")
                .contains("https://images.unsplash.com/g2");
    }

    @Test
    void aLegacySingleUrlCacheEntryIsIgnoredAndTheSearchIsRepeated() throws Exception {
        // v1 keş dəyəri: tək URL. Oxunmamalı, çünki format dəyişib.
        when(valueOperations.get("image:v2:greenland")).thenReturn("https://images.unsplash.com/old");
        server.enqueue(results(photo("fresh", "New icebergs in Greenland", "greenland ice arctic")));

        assertThat(service.resolveImageCandidates(DestinationImageQuery.of("Greenland", null, null, Set.of())))
                .extracting(ImageCandidateResponse::url)
                .containsExactly("https://images.unsplash.com/fresh");
    }

    @Test
    void buildsACacheKeyFromTheDestinationAndCountry() {
        when(valueOperations.get(anyString())).thenReturn(cachedJson("cached"));

        service.resolveImageUrl(DestinationImageQuery.of("Dolomites", "Italy", null, Set.of()));
        verify(valueOperations).get("image:v2:dolomites-italy");

        service.resolveImageUrl(DestinationImageQuery.of("Greenland", null, null, Set.of()));
        verify(valueOperations).get("image:v2:greenland");
    }

    @Test
    void aCachedNegativeResultIsNotLookedUpAgain() {
        // Empty string is the "we already know there is no image" marker.
        when(valueOperations.get(anyString())).thenReturn("");

        assertThat(service.resolveImageCandidates(DestinationImageQuery.of("Nowhere", null, null, Set.of())))
                .isEmpty();
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void cachesTheShortTtlForAMiss() {
        for (int i = 0; i < 4; i++) {
            server.enqueue(emptyResults());
        }

        service.resolveImageUrl(DestinationImageQuery.of("Unfindable", null, null, Set.of()));

        verify(valueOperations).set(
                ArgumentMatchers.eq("image:v2:unfindable"),
                ArgumentMatchers.eq(""),
                ArgumentMatchers.eq(Duration.ofHours(6))
        );
    }

    @Test
    void cachesTheLongTtlForAFoundImage() {
        server.enqueue(results(photo("greenland", "Icebergs in Greenland", "greenland ice arctic")));

        service.resolveImageUrl(DestinationImageQuery.of("Greenland", null, null, Set.of()));

        ArgumentCaptor<String> cached = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(ArgumentMatchers.eq("image:v2:greenland"), cached.capture(),
                ArgumentMatchers.eq(Duration.ofDays(30)));
        assertThat(cached.getValue()).contains("https://images.unsplash.com/greenland");
    }

    // ------------------------------------------------------------------
    // Graceful degradation - the whole point of this feature.
    // ------------------------------------------------------------------

    @Test
    void redisBeingDownDoesNotStopImageResolution() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("redis is down"));
        server.enqueue(results(photo("patagonia", "Glaciers in Patagonia", "patagonia mountains glacier")));

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Patagonia", null, null, Set.of())))
                .isEqualTo("https://images.unsplash.com/patagonia");
    }

    @Test
    void aFailedCacheWriteDoesNotBreakTheResolvedImage() {
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("redis is down"));
        server.enqueue(results(photo("patagonia", "Glaciers in Patagonia", "patagonia mountains glacier")));

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Patagonia", null, null, Set.of())))
                .isEqualTo("https://images.unsplash.com/patagonia");
    }

    @Test
    void unsplashErrorsReturnNullRatherThanPropagating() {
        server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"errors\":[\"Rate Limit Exceeded\"]}"));
        for (int i = 0; i < 5; i++) {
            server.enqueue(new MockResponse().setResponseCode(500));
        }

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Greenland", null, null, Set.of()))).isNull();
    }

    @Test
    void aMalformedResponseIsTreatedAsNoResult() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("not json at all"));
        for (int i = 0; i < 5; i++) {
            server.enqueue(emptyResults());
        }

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Greenland", null, null, Set.of()))).isNull();
    }

    @Test
    void withoutAnAccessKeyNoNetworkCallIsMadeAtAll() {
        properties.getUnsplash().setAccessKey("");
        server.enqueue(results(photo("never", "Never used", "never used")));

        assertThat(service.resolveImageUrl(DestinationImageQuery.of("Greenland", null, null, Set.of()))).isNull();
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void blankDestinationNamesAreRejectedWithoutTouchingRedisOrUnsplash() {
        assertThat(service.resolveImageUrl(null)).isNull();
        assertThat(service.resolveImageUrl("   ", "Italy")).isNull();
        assertThat(service.resolveImageUrl(DestinationImageQuery.of(null, null, null, Set.of()))).isNull();

        verify(redisTemplate, never()).opsForValue();
        assertThat(server.getRequestCount()).isZero();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Bir foto: URL + təsvir + teqlər (Unsplash metadata-sının sadələşdirilmiş forması). */
    private static String photo(String id, String description, String tags) {
        return "{\"id\":\"" + id + "\","
                + "\"width\":2400,\"height\":1600,"
                + "\"alt_description\":\"" + description + "\","
                + "\"urls\":{\"regular\":\"https://images.unsplash.com/" + id + "\"},"
                + "\"tags\":[" + tagList(tags) + "]}";
    }

    private static String tagList(String tags) {
        StringBuilder sb = new StringBuilder();
        for (String tag : tags.split(" ")) {
            if (tag.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append("{\"title\":\"").append(tag).append("\"}");
        }
        return sb.toString();
    }

    private static MockResponse results(String... photos) {
        StringBuilder json = new StringBuilder("{\"total\":").append(photos.length).append(",\"results\":[");
        for (int i = 0; i < photos.length; i++) {
            json.append(i > 0 ? "," : "").append(photos[i]);
        }
        json.append("]}");
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(json.toString());
    }

    /** Keşdə saxlanan namizəd JSON-u (tək foto). */
    private static String cachedJson(String id) {
        return "[{\"id\":\"" + id + "\","
                + "\"url\":\"https://images.unsplash.com/" + id + "\","
                + "\"fullUrl\":\"https://images.unsplash.com/" + id + "\"}]";
    }

    private static MockResponse emptyResults() {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"total\":0,\"results\":[]}");
    }
}