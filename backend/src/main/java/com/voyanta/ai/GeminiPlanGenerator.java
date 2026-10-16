//package com.voyanta.ai;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.voyanta.ai.dto.request.AiRequest;
//import com.voyanta.ai.dto.response.AiResponse;
//import com.voyanta.common.config.VoyantaProperties;
//import com.voyanta.common.exception.ApiException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientRequestException;
//import reactor.util.retry.Retry;
//
//import java.time.Duration;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeoutException;
//
//@Slf4j
//@Component
//class GeminiPlanGenerator implements AiPlanGenerator {
//
//    private final WebClient webClient;
//    private final VoyantaProperties properties;
//    private final PromptBuilder promptBuilder;
//    private final ObjectMapper objectMapper;
//
//    GeminiPlanGenerator(
//            WebClient.Builder webClientBuilder,
//            VoyantaProperties properties,
//            PromptBuilder promptBuilder,
//            ObjectMapper objectMapper
//    ) {
//        this.properties = properties;
//        this.promptBuilder = promptBuilder;
//        this.objectMapper = objectMapper;
//        this.webClient = webClientBuilder
//                .baseUrl(properties.getAi().getBaseUrl())
//                .build();
//    }
//
//    @Override
//    public AiResponse generate(AiRequest request) {
//        String apiKey = properties.getAi().getApiKey();
//        if (apiKey == null || apiKey.isBlank()) {
//            log.error("AI_API_KEY boşdur, mühit dəyişəni oxunmayıb");
//            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PARSE_ERROR", "AI konfiqurasiyası tamamlanmayıb");
//        }
//
//        Map<String, Object> requestBody = Map.of(
//                "contents", List.of(Map.of(
//                        "role", "user",
//                        "parts", List.of(Map.of("text", promptBuilder.buildUserMessage(request)))
//                )),
//                "systemInstruction", Map.of(
//                        "parts", List.of(Map.of("text", PromptBuilder.SYSTEM_PROMPT))
//                ),
//                "generationConfig", Map.of(
//                        "responseMimeType", "application/json",
//                        "responseSchema", toGeminiSchema(promptBuilder.outputSchema())
//                )
//        );
//
//        JsonNode response;
//        try {
//            response = webClient.post()
//                    .uri("/v1beta/models/{model}:generateContent", properties.getAi().getModel())
//                    .header("x-goog-api-key", apiKey)   // açar URL-də yox, header-dədir
//                    .bodyValue(requestBody)
//                    .retrieve()
//                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
//                            .defaultIfEmpty("")
//                            .map(body -> new GeminiHttpException(r.statusCode().value(), body)))
//                    .bodyToMono(JsonNode.class)
//                    .timeout(properties.getAi().getTimeout())
//                    .retryWhen(Retry.backoff(properties.getAi().getMaxRetries(), Duration.ofSeconds(1))
//                            .filter(GeminiPlanGenerator::isRetryable)
//                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
//                    .block();
//        } catch (GeminiHttpException e) {
//            log.error("Gemini HTTP {} xətası: {}", e.status, truncate(e.body));
//            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PARSE_ERROR", "AI servisi xəta qaytardı");
//        } catch (Exception e) {
//            log.error("Gemini çağırışı uğursuz oldu", e);
//            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PARSE_ERROR", "AI servisinə qoşulmaq mümkün olmadı");
//        }
//
//        return extractContent(response);
//    }
//
//    private AiResponse extractContent(JsonNode response) {
//        if (response == null) {
//            log.error("Gemini boş cavab qaytardı (null)");
//            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "AI-dan cavab alınmadı");
//        }
//
//        String content = response
//                .path("candidates").path(0)
//                .path("content").path("parts").path(0)
//                .path("text").asText(null);
//
//        if (content == null || content.isBlank()) {
//            log.error("Gemini cavabında mətn yoxdur. finishReason={}, promptFeedback={}",
//                    response.path("candidates").path(0).path("finishReason").asText("?"),
//                    response.path("promptFeedback"));
//            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE", "AI-dan boş cavab gəldi");
//        }
//
//        try {
//            return objectMapper.readValue(content, AiResponse.class);
//        } catch (Exception e) {
//            log.error("Gemini cavabı AiResponse-a çevrilmədi. Cavab: {}", truncate(content), e);
//            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_PARSE_ERROR", "AI cavabı parse olunmadı");
//        }
//    }
//
//    /** Gemini-nin dəstəkləmədiyi "additionalProperties"-i silir, "type" dəyərlərini böyük hərfə çevirir. */
//    private Object toGeminiSchema(Object node) {
//        if (node instanceof Map<?, ?> map) {
//            Map<String, Object> out = new LinkedHashMap<>();
//            map.forEach((k, v) -> {
//                String key = String.valueOf(k);
//                if (key.equals("additionalProperties")) {
//                    return;
//                }
//                if (key.equals("type") && v instanceof String s) {
//                    out.put(key, s.toUpperCase());
//                } else {
//                    out.put(key, toGeminiSchema(v));
//                }
//            });
//            return out;
//        }
//        if (node instanceof List<?> list) {
//            return list.stream().map(this::toGeminiSchema).toList();
//        }
//        return node;
//    }
//
//    private static boolean isRetryable(Throwable t) {
//        if (t instanceof GeminiHttpException g) {
//            return g.status == 429 || g.status >= 500;
//        }
//        return t instanceof TimeoutException || t instanceof WebClientRequestException;
//    }
//
//    private static String truncate(String s) {
//        return s == null ? "" : (s.length() > 1500 ? s.substring(0, 1500) + "..." : s);
//    }
//
//    private static final class GeminiHttpException extends RuntimeException {
//        final int status;
//        final String body;
//
//        GeminiHttpException(int status, String body) {
//            super("Gemini HTTP " + status);
//            this.status = status;
//            this.body = body;
//        }
//    }
//}