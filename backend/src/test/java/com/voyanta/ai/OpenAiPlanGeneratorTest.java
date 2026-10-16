package com.voyanta.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.BudgetTier;
import com.voyanta.survey.enums.CompanionType;
import com.voyanta.survey.enums.HotelType;
import com.voyanta.survey.enums.InterestType;
import com.voyanta.survey.enums.MealPreference;
import com.voyanta.survey.enums.TripPurpose;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiPlanGeneratorTest {

    private MockWebServer server;
    private VoyantaProperties properties;
    private OpenAiPlanGenerator generator;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        properties = new VoyantaProperties();
        properties.getAi().setApiKey("sk-test");
        properties.getAi().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getAi().setModel("gpt-4o");
        properties.getAi().setTimeout(Duration.ofSeconds(3));
        properties.getAi().setMaxRetries(0);

        generator = new OpenAiPlanGenerator(
                WebClient.builder(),
                properties,
                new PromptBuilder(new ObjectMapper()),
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void parsesStructuredChatCompletionJsonWithoutCallingTheRealProvider() throws Exception {
        String itineraryJson = """
                {"destination":"Santorini","days":[{"dayNumber":1,"items":[{"time":"09:00","title":"Walk","description":"Old town","category":"activity"}]}],"budgetSummary":{"accommodation":100,"food":40,"transport":20,"activities":30,"total":190}}
                """;
        String escaped = itineraryJson.replace("\"", "\\\"").replace("\n", "");
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"choices":[{"message":{"content":"%s"}}]}
                        """.formatted(escaped)));

        AiResponse response = generator.generate(sampleRequest());
        assertThat(response.destination()).isEqualTo("Santorini");
        assertThat(response.days()).hasSize(1);
        assertThat(response.budgetSummary().total()).isNotNull();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-test");
        assertThat(recorded.getBody().readUtf8()).contains("gpt-4o").contains("generate_itinerary");
    }

    @Test
    void emptyContentIsMappedToApiException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"\"}}]}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_EMPTY_RESPONSE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    @Test
    void unparsableContentIsMappedToApiException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo("AI_PARSE_ERROR"));
    }

    // ------------------------------------------------------------------
    // Production-dakı 429 hadisəsinin regression testləri.
    // Əvvəl bu halda çıplaq WebClientResponseException$TooManyRequests xaricə
    // sızırdı, heç bir API sözüyü yox idi və təkrar cəhd strategiyası
    // filtrsiz idi (yəni hər xəta, o cümlədən 400, eyni sayda təkrar olunurdu).
    // ------------------------------------------------------------------

    @Test
    void rateLimitIsMappedToServiceUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_UNAVAILABLE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void serverErrorIsMappedToServiceUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("upstream down"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_UNAVAILABLE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void clientErrorIsNotRetriedAndIsNotAnAvailabilityProblem() {
        properties.getAi().setMaxRetries(2);
        server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"bad request\"}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_REQUEST_REJECTED");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });

        // Əvvəlki filtrsiz Retry 400-ü də 3 dəfə təkrarlayırdı; indi cəmi 1 sorğu gedir.
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void blankApiKeyFailsFastWithoutCallingTheProvider() {
        properties.getAi().setApiKey("  ");

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_UNAVAILABLE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });

        assertThat(server.getRequestCount()).isZero();
    }

    // ------------------------------------------------------------------
    // Retry-count and retry-eligibility rules. These protect the OpenAI
    // account itself: a 400/401/403 must never be retried, and a retryable
    // error must be attempted exactly maxRetries+1 times (no more).
    // ------------------------------------------------------------------

    @Test
    void rateLimitIsRetriedExactlyMaxRetriesTimesThenSucceeds() {
        properties.getAi().setMaxRetries(2);
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));
        server.enqueue(okResponse());

        AiResponse response = generator.generate(sampleRequest());

        assertThat(response.destination()).isEqualTo("Santorini");
        assertThat(server.getRequestCount())
                .as("2 retries means 3 attempts in total, then success")
                .isEqualTo(3);
    }

    @Test
    void rateLimitIsRetriedUpToTheLimitThenMappedToServiceUnavailable() {
        properties.getAi().setMaxRetries(2);
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo("AI_UNAVAILABLE"));

        assertThat(server.getRequestCount())
                .as("retries must be bounded, never an unbounded storm against a rate-limited account")
                .isEqualTo(3);
    }

    @Test
    void serverErrorIsRetriedAndThenMappedToServiceUnavailable() {
        properties.getAi().setMaxRetries(1);
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_UNAVAILABLE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });

        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void timeoutIsRetriedAndThenSucceedsWhenTheProviderRecovers() {
        properties.getAi().setMaxRetries(2);
        properties.getAi().setTimeout(Duration.ofMillis(300));

        // Counting Dispatcher (not getRequestCount(), which is unreliable when the client
        // aborts a delayed response). The first call stalls past the client timeout; the
        // next one answers normally, so success proves the timeout was retried.
        AtomicInteger served = new AtomicInteger();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (served.incrementAndGet() == 1) {
                    return new MockResponse()
                            .setHeader("Content-Type", "application/json")
                            .setBody("{\"choices\":[]}")
                            .setBodyDelay(3, TimeUnit.SECONDS);
                }
                return okResponse();
            }
        });

        AiResponse response = generator.generate(sampleRequest());

        assertThat(response.destination())
                .as("a transient timeout must be retried rather than failed")
                .isEqualTo("Santorini");
        assertThat(served.get()).isEqualTo(2);
    }

    @Test
    void persistentTimeoutIsRetriedUpToTheLimitAndThenMappedToServiceUnavailable() {
        properties.getAi().setMaxRetries(1);
        properties.getAi().setTimeout(Duration.ofMillis(300));

        AtomicInteger served = new AtomicInteger();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                served.incrementAndGet();
                return new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"choices\":[]}")
                        .setBodyDelay(3, TimeUnit.SECONDS);
            }
        });

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode())
                            .as("a timeout is a provider availability problem, not a bad request")
                            .isEqualTo("AI_UNAVAILABLE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });

        assertThat(served.get())
                .as("retries must be bounded: maxRetries=1 means exactly 2 attempts")
                .isEqualTo(2);
    }

    @Test
    void connectionFailureIsMappedToServiceUnavailableWithoutLeakingTheApiKey() throws IOException {
        // Server bagli deyil -> connection refused.
        MockWebServer dead = new MockWebServer();
        dead.start();
        String baseUrl = dead.url("/").toString().replaceAll("/$", "");
        dead.shutdown();

        properties.getAi().setMaxRetries(0);
        properties.getAi().setBaseUrl(baseUrl);
        generator = new OpenAiPlanGenerator(
                WebClient.builder(), properties, new PromptBuilder(new ObjectMapper()), new ObjectMapper());

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo("AI_UNAVAILABLE");
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(api.getMessage())
                            .as("the API key must never appear in an error surfaced to the client")
                            .doesNotContain(properties.getAi().getApiKey());
                });
    }

    @Test
    void badCredentialsAreNotRetried() {
        // 401/403 don't improve by trying again; retrying them wastes quota and time.
        for (int status : new int[]{401, 403}) {
            properties.getAi().setMaxRetries(3);
            int before = server.getRequestCount();
            server.enqueue(new MockResponse().setResponseCode(status).setBody("{\"error\":\"forbidden\"}"));

            assertThatThrownBy(() -> generator.generate(sampleRequest()))
                    .as("HTTP " + status)
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo("AI_REQUEST_REJECTED"));

            assertThat(server.getRequestCount() - before)
                    .as("HTTP " + status + " must be attempted exactly once")
                    .isEqualTo(1);
        }
    }

    @Test
    void zeroRetriesMeansExactlyOneAttempt() {
        properties.getAi().setMaxRetries(0);
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate_limit\"}"));

        assertThatThrownBy(() -> generator.generate(sampleRequest()))
                .isInstanceOf(ApiException.class);

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void apiKeyIsSentInTheAuthorizationHeaderOnEveryAttempt() throws InterruptedException {
        server.enqueue(okResponse());
        server.enqueue(okResponse());

        generator.generate(sampleRequest());
        generator.generate(sampleRequest());

        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer sk-test");
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer sk-test");
    }

    /**
     * The regression test for "the planner always returns the same destinations":
     * the survey answers MUST reach the provider inside the user message, and two
     * different answer combinations MUST produce two different prompts — otherwise
     * the model can only ever anchor on whatever the system prompt mentions.
     */
    @Test
    void differentSurveyAnswersProduceDifferentPromptsToTheProvider() throws Exception {
        server.enqueue(okResponse());
        server.enqueue(okResponse());

        AiRequest beachTrip = new AiRequest(
                Set.of(InterestType.SEA),
                CompanionType.COUPLE,
                null,
                new Budget(BudgetTier.PREMIUM, null),
                new TravelDates(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), null),
                HotelType.FIVE_STAR,
                MealPreference.ALL_INCLUSIVE,
                Set.of(TripPurpose.HONEYMOON)
        );
        AiRequest mountainTrip = new AiRequest(
                Set.of(InterestType.NATURE),
                CompanionType.SOLO,
                null,
                new Budget(BudgetTier.ECONOMY, null),
                new TravelDates(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), null),
                HotelType.THREE_STAR,
                MealPreference.NO_MEALS,
                Set.of(TripPurpose.ADVENTURE)
        );

        generator.generate(beachTrip);
        generator.generate(mountainTrip);

        String first = userMessageOf(server.takeRequest());
        String second = userMessageOf(server.takeRequest());

        assertThat(first)
                .as("different answers must produce a different user message")
                .isNotEqualTo(second)
                .contains("SEA", "COUPLE", "PREMIUM", "FIVE_STAR", "ALL_INCLUSIVE", "HONEYMOON");
        assertThat(second)
                .contains("NATURE", "SOLO", "ECONOMY", "THREE_STAR", "NO_MEALS", "ADVENTURE");

        // The system prompt must not name any concrete destination that could anchor
        // the model onto the same few places regardless of the answers.
        assertThat(PromptBuilder.SYSTEM_PROMPT)
                .doesNotContainIgnoringCase("maldives", "maldiv", "santorini",
                        "florence", "florensiya", "florida", "bali", "paris");
    }

    /** Extracts the user message (second chat message) from a recorded provider request. */
    private static String userMessageOf(RecordedRequest request) throws IOException {
        JsonNode body = new ObjectMapper().readTree(request.getBody().readUtf8());
        return body.path("messages").path(1).path("content").asText();
    }

    private static MockResponse okResponse() {
        String json = "{\"destination\":\"Santorini\",\"days\":[{\"dayNumber\":1,\"items\":[{\"time\":\"09:00\",\"title\":\"Walk\",\"description\":\"Old town\",\"category\":\"activity\"}]}],\"budgetSummary\":{\"accommodation\":100,\"food\":40,\"transport\":20,\"activities\":30,\"total\":190}}";
        String escaped = json.replace("\"", "\\\"").replace("\n", "");
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"%s\"}}]}".formatted(escaped));
    }

    private static AiRequest sampleRequest() {
        return new AiRequest(
                Set.of(InterestType.SEA),
                CompanionType.SOLO,
                null,
                new Budget(BudgetTier.ECONOMY, null),
                new TravelDates(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), null),
                HotelType.THREE_STAR,
                MealPreference.NO_MEALS,
                Set.of(TripPurpose.ADVENTURE)
        );
    }
}
