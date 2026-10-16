package com.voyanta.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Type-safe configuration properties class bound to the "voyanta:" block of application.yaml.
 * It groups the app settings: JWT, AI provider, survey session TTL, rate limits,
 * file storage, OAuth client ids, homepage values and allowed CORS origins.
 *
 * application.yml-dəki "voyanta:" bloku ilə eyni struktur.
 * Hər yerdə @Value("${...}") səpələmək əvəzinə, bunu inject edib
 * properties.getJwt().getSecret() kimi tipli şəkildə istifadə edirik.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "voyanta")
public class VoyantaProperties {

    private Jwt jwt = new Jwt();
    private Ai ai = new Ai();
    private Survey survey = new Survey();
    private RateLimit rateLimit = new RateLimit();
    private Storage storage = new Storage();
    private OAuth oauth = new OAuth();
    private Unsplash unsplash = new Unsplash();
    private Homepage homepage = new Homepage();
    private Cors cors = new Cors();

    /** Holds the JWT signing secret and the lifetimes of access and refresh tokens. */
    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private Duration accessTokenTtl;
        private Duration refreshTokenTtl;
    }

    /** Holds the AI provider settings used for plan generation: API key, base URL, model, timeout and retry count. */
    @Getter
    @Setter
    public static class Ai {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Duration timeout;
        private int maxRetries;
    }

    /** Holds how long an unfinished survey session stays alive in Redis. */
    @Getter
    @Setter
    public static class Survey {
        private Duration sessionTtl;
    }

    /** Holds the request limits: daily plans for anonymous and authenticated users, plus the global AI calls per minute. */
    @Getter
    @Setter
    public static class RateLimit {
        private int anonymousPlansPerDay;
        private int authenticatedPlansPerDay;
        private int aiRequestsPerMinuteGlobal;
    }

    /** Holds the S3-compatible object storage settings (bucket, endpoint, keys, region) used for images. */
    @Getter
    @Setter
    public static class Storage {
        private String provider;
        private String bucket;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String region;
    }

    /** Groups the client ids of the social login providers used for sign-in. */
    @Getter
    @Setter
    public static class OAuth {
        private Google google = new Google();
        private Apple apple = new Apple();

        /** Holds the Google OAuth client id used to verify Google sign-in tokens. */
        @Getter
        @Setter
        public static class Google {
            private String clientId;
        }

        /** Holds the Apple OAuth client id used to verify Apple sign-in tokens. */
        @Getter
        @Setter
        public static class Apple {
            private String clientId;
        }
    }

    /**
     * Holds the Unsplash (image provider) settings. The access key is read from the
     * UNSPLASH_ACCESS_KEY environment variable and stays on the backend: it is only ever
     * sent as an "Authorization: Client-ID" header and is never part of an API response.
     */
    @Getter
    @Setter
    public static class Unsplash {
        private String accessKey;
        private String baseUrl;
        private String searchPath;

        // Search tuning: "landscape" is what the destination cards need, and
        // content_filter=high keeps explicit/low-quality results out.
        private String orientation;
        private String contentFilter;
        private int perPage;

        private Duration timeout;

        // How long a resolved destination image stays in Redis, and how long a
        // "this destination has no image" negative result is remembered (shorter,
        // so a destination that gains photos later is picked up again).
        private Duration cacheTtl;
        private Duration negativeCacheTtl;
    }

    /** Holds the homepage values: the displayed average rating and how long homepage stats are cached. */
    @Getter
    @Setter
    public static class Homepage {
        // Real reytinq (rəy) sistemi qurulana qədər admin-configurable sabit dəyər —
        // application.yml-də dəyişdirilir, kod dəyişmir.
        private BigDecimal avgRating;
        private Duration statsCacheTtl;
    }

    /** Holds the list of frontend origins that are allowed to call the API; read by CorsConfig. */
    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }
}