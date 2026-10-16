package com.voyanta.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.ai.dto.response.AiResponse;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Diqqət: package-private — plan modulu yalnız AiPlanGenerator interfeysini görür.
 * OpenAI-nin "Structured Outputs" (response_format: json_schema, strict: true) rejimi
 * istifadə olunur — Anthropic-in tool_choice-una konseptual alternativ, eyni məqsədlə:
 * modelin sərbəst mətn yox, bizim schema-mıza tam uyğun JSON qaytarması.
 *
 * MÜVƏQQƏTİ SÖNDÜRÜLÜB — AI provider Gemini-yə keçib (bax GeminiPlanGenerator.java).
 * Geri qayıtmaq lazım olsa, aşağıdakı @Component-i aç VƏ GeminiPlanGenerator-dəkini
 * kommentə al — eyni interfeysin iki aktiv bean-i Spring-i startup-da çökdürər.
 *
 * DIQQƏT: Bu klass yalnız plan generasiyası tərəfindən çağırılır. O, autentifikasiya
 * ilə heç bir şəkildə əlaqəli deyil və OLMAYA da bilmir — bax PlanGenerationWorker.
 * OpenAI-nin 429/5xx verməsi planı FAILED edir, amma heç vaxt giriş sorğusunu 401
 * etmir. Aşağıdakı xəta tərcidi elə bunu qoruyur: provider xətası əvvəlki kimi çıplaq
 * WebClientResponseException kimi qalmasın, həm də "provider hazır deyil" (503)
 * kimi düzgün təsnif edilsin.
 */
@Slf4j
@Component
@Profile("!test")
class OpenAiPlanGenerator implements AiPlanGenerator {

    private static final int MAX_LOGGED_BODY_LENGTH = 500;

    private final WebClient webClient;
    private final VoyantaProperties properties;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    OpenAiPlanGenerator(
            WebClient.Builder webClientBuilder,
            VoyantaProperties properties,
            PromptBuilder promptBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(properties.getAi().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getAi().getApiKey())
                .build();
    }

    @Override
    public AiResponse generate(AiRequest request) {
        String apiKey = properties.getAi().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            // Provider-a "Bearer " (boş açar) ilə getmək 429/401 qaytarır və 3 boş cəhdə
            // pul xərcləyir. Mühit dəyişəni oxunmaması ayrıca problemdir.
            log.error("voyanta.ai.api-key boşdur — AI provider çağırılmır");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "AI servisi konfiqurasiya edilməyib");
        }

        // İstifadəçinin cavabları prompt-a düzgün düşürmü — bunu DEBUG səviyyəsində
        // görmək olar. Heç bir gizli yox alternativ cavab yoxdur: provider xətası
        // planı FAILED edir və yuxarıda error olaraq loglanır.
        String userMessage = promptBuilder.buildUserMessage(request);
        log.debug("AI plan sorğusunun istifadəçi mesajı: {}", userMessage);

        Map<String, Object> requestBody = Map.of(
                "model", properties.getAi().getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", PromptBuilder.SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userMessage)
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "generate_itinerary",
                                "strict", true,
                                "schema", promptBuilder.outputSchema()
                        )
                )
        );

        return extractContent(callProvider(requestBody));
    }

    private JsonNode callProvider(Map<String, Object> requestBody) {
        try {
            Mono<JsonNode> call = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(properties.getAi().getTimeout());

            return (properties.getAi().getMaxRetries() > 0
                    ? call.retryWhen(retryPolicy())
                    : call)
                    .block();
        } catch (WebClientResponseException e) {
            throw toApiException(e);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            // Timeout, DNS/TCP qəxrası, bağlantı qopması — heç biri "plan yoxdur" demək
            // deyil, sadəcə provider hazır deyil. Çıplaq səbəb dışarı sıxmasın.
            log.error("AI provider-ə qoşulmaq mümkün olmadı: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "AI servisinə qoşulmaq mümkün olmadı");
        }
    }

    /**
     * Yalnız həqiqətən təkrar cəhdə layih olan xətaları təkrar edir.
     *
     * Əvvəlki versiyanın {@code Retry.backoff(n, 1s)} filteri yox idi — yəni HƏR xəta,
     * o cümlədən 400 "bad request" və 401 "bad API key" də, eyni cəhd sayı ilə təkrar
     * olunurdu. 429 halında isə bu, onsuz da limitlənmiş hesabı 3 dəfə daha vurmaq
     * demək idi: rate limit xətasını rate limiti daha da pisləşdirmək.
     *
     * Təkrar edilənlər: 429, 5xx, timeout və bağlantı səviyyəli xətalar.
     * Təkrar EDİLMƏYƏN: digər 4xx — təkrar cəhd heç nəyi düzəltmir, sadəcə zərər verir.
     *
     * {@code onRetryExhaustedThrow} vacibdir: defoltda Reactor RetryExhaustedException
     * atır və əsas səbəb (məs. 429) itir. Biz əsas səbəbi qaytarmaq istəyirik ki,
     * {@link #toApiException(WebClientResponseException)} onu 503-ə tərcid edə bilsin.
     */
    private Retry retryPolicy() {
        return Retry.backoff(properties.getAi().getMaxRetries(), Duration.ofSeconds(1))
                .filter(this::isRetryable)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private boolean isRetryable(Throwable error) {
        WebClientResponseException http = findCause(error, WebClientResponseException.class);
        if (http != null) {
            return http.getStatusCode().value() == 429 || http.getStatusCode().is5xxServerError();
        }
        return findCause(error, TimeoutException.class) != null
                || findCause(error, WebClientRequestException.class) != null
                || findCause(error, IOException.class) != null;
    }

    private static <T extends Throwable> T findCause(Throwable error, Class<T> type) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * Provider-in HTTP xətasını bizim API sözüyümüzə tərcid edir.
     *
     * 429/5xx -> 503 AI_UNAVAILABLE        ("provider hazır deyil / limit bitib")
     * 4xx    -> 502 AI_REQUEST_REJECTED   ("səhdimiz: açar/sorğu problemlidir")
     *
     * Hər iki halda da cavab düzgün ApiException olur — əvvəlki kimi çıplaq
     * WebClientResponseException GlobalExceptionHandler-a düşüb 500 yaratmır.
     */
    private ApiException toApiException(WebClientResponseException e) {
        int status = e.getStatusCode().value();
        String detail = truncate(e.getResponseBodyAsString());

        if (status == 429 || e.getStatusCode().is5xxServerError()) {
            log.warn("AI provider cavabı {} (limit/ehtiyat problemi ola bilər): {}", status, detail);
            return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "AI servisi hazır deyil (provider HTTP " + status + "), bir az sonra yenidən yoxla");
        }

        // Diqqət: burada heç bir açar (Authorization header) loglanmır —
        // yalnız provider-in qaytardığı cavab gövdəsi, o da qısa şəkildə.
        log.error("AI provider sorğunu qəbul etmədi: HTTP {} — {}", status, detail);
        return new ApiException(HttpStatus.BAD_GATEWAY, "AI_REQUEST_REJECTED",
                "AI sorğusu qəbul edilmədi (provider HTTP " + status + ")");
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_LOGGED_BODY_LENGTH
                ? value
                : value.substring(0, MAX_LOGGED_BODY_LENGTH) + "...";
    }

    private AiResponse extractContent(JsonNode response) {
        if (response == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "AI-dan cavab alınmadı");
        }

        String content = response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "AI-dan boş cavab gəldi");
        }

        try {
            // Diqqət: OpenAI-də content hazır JSON obyekt DEYİL, JSON STRING-dir —
            // Anthropic-in tool_use.input-undan fərqli olaraq, əlavə parse addımı lazımdır.
            return objectMapper.readValue(content, AiResponse.class);
        } catch (Exception e) {
            // Parse səssiz baş verə bilməz: boş/gözlənilmən cavab gələndə nəyin
            // gəldiyi logda görünür ki, belə problem gizlənməsin.
            log.error("AI cavabı AiResponse-a çevrilmədi. Cavab: {}", truncate(content), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PARSE_ERROR", "AI cavabı parse olunmadı");
        }
    }
}