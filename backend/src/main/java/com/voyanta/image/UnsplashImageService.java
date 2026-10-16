package com.voyanta.image;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.common.config.VoyantaProperties;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * ImageService-in Unsplash Search Photos API ilə tətbiq olunan versiyası.
 *
 * Axın: Redis keşi (namizəd SİYAHISI) -> yoxdursa {DestinationImageQueryBuilder}
 * sorğuları -> hər sorğunun nəticələri {UnsplashResultRanker} ilə sıralanır ->
 * birinci uyğun sorğunun relevant namizədləri. Uğursuzluq heç vaxt istisna deyil —
 * sadəcə növbəti boş sorğu, nəticə isə boş siyahıdır.
 *
 * <p><b>Niyə keş bir URL deyil, siyahıdır?</b> Keçmişdə bir şəkil 30 gün keşdə
 * sabit qalırdı, bu da (a) eyni şəklin təkrar görünməsinə və (b) səhv şəklin
 * bir ay ərzində qalmasına səbəb olurdu. İndi keş <b>bütün relevant namizədləri</b>
 * saxlayır, belədə həm çeşidlilik, həm də düzəldilmə imkanı qalır.
 *
 * Üç qorxu (bütünü "plan qırılmasın" məqsədi ilə) try/catch içindədir:
 *  1) Redis düşməyib         -> keş atlanır, birbaşa Unsplash-a gedilir;
 *  2) Unsplash çağırışı xətası / limit bitib -> növbəti boş sorğu, boş siyahı;
 *  3) açar mühit dəyişəni oxunmayıb          -> heç bir şəbəkə sorğusu atılmır.
 *
 * Açar (`voyanta.unsplash.access-key`) YALNIZ backend-də qalır: o, sorğunun
 * Authorization başlığında göndərilir və heç bir cavaba yazılmır. Loglarda da
 * açarın özü deyil, yalnız xətanın sətri yazılır.
 */
@Slf4j
@Service
public class UnsplashImageService implements ImageService {

    // Mövcud Redis adlandırma üslubuna uyğun: "survey:session:", "plan:progress:",
    // "homepage:stats", "rate:" — burada da "image:<slug>".
    // v2 = namizəd siyahısı + ağıllı sorğu (v1 tək URL idi və təkrar yaradırdı).
    private static final String CACHE_KEY_PREFIX = "image:v2:";

    // Tapılmış şəkil yoxdursa keşdə boş sətir saxlayırıq. Beləcə hər sorğuda
    // eyni hopeless ad (məs. "Svalbard") yenidən API-yə vurulmur.
    private static final String NOT_FOUND_MARKER = "";

    // Kart ölçüsü üçün "regular" (~1080px) yetərlidir; tam ekran baxış üçün
    // "full" (~2000px) ayrıca saxlanılır.
    private static final String CARD_URL_SIZE = "regular";
    private static final String[] URL_FALLBACK_SIZES = {"regular", "small", "full", "thumb"};

    // Bir destinationsa nə qədər namizəd qaytarılır. Bu, frontend-ə fərqli şəkil
    // seçmək üçün kifayət qədər çeşidlilik verir, amma cavabı şişirtmir.
    private static final int MAX_CANDIDATES = 6;

    private final WebClient webClient;
    private final VoyantaProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DestinationImageQueryBuilder queryBuilder;
    private final UnsplashResultRanker ranker;

    public UnsplashImageService(
            WebClient.Builder webClientBuilder,
            VoyantaProperties properties,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            DestinationImageQueryBuilder queryBuilder,
            UnsplashResultRanker ranker
    ) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.queryBuilder = queryBuilder;
        this.ranker = ranker;
        this.webClient = webClientBuilder
                .baseUrl(properties.getUnsplash().getBaseUrl())
                .build();
    }

    @Override
    public List<ImageCandidateResponse> resolveImageCandidates(DestinationImageQuery query) {
        if (query == null) {
            return List.of();
        }

        String cleanDestination = clean(query.destination());
        String cleanCountry = clean(query.country());
        // Ad boşdursa heç nə axtarılmır — əvvəlki davranışın qorunması: keşə də
        // toxunulmur, çünki onsuz da hansı açar istifadə olunacaq məlum deyil.
        if (cleanDestination == null) {
            return List.of();
        }

        // Açar yoxdursa heç bir şəbəkə sorğusu atılmır — test və lokal mühitdə
        // bu, "plan generasiyası testləri real API-yə getməsin" qaydasını təmin edir.
        if (isBlank(properties.getUnsplash().getAccessKey())) {
            log.debug("voyanta.unsplash.access-key boşdur — şəkil axtarışı aparılmır, destination={}",
                    cleanDestination);
            return List.of();
        }

        DestinationImageQuery normalised = new DestinationImageQuery(
                cleanDestination, cleanCountry, clean(query.tag()), query.interests());

        String cacheKey = CACHE_KEY_PREFIX + cacheSlug(cleanDestination, cleanCountry);
        List<ImageCandidateResponse> cached = readFromCache(cacheKey);
        if (cached != null) {
            // Boş sətir = keşdə "tapılmadı" qeydi var; API-yə yenidən getmərik.
            return cached;
        }

        List<ImageCandidateResponse> resolved = searchUnsplash(normalised);
        writeToCache(cacheKey, resolved, resolved.isEmpty());
        return resolved;
    }

    /**
     * Sorğular zənciri: ən konkretdən ən ümumiyə. Hər sorğunun nəticələri ranker ilə
     * sıralanır; ilk <b>relevant</b> nəticə tapılan sorğu dayandırılır.
     */
    private List<ImageCandidateResponse> searchUnsplash(DestinationImageQuery query) {
        List<String> queries = queryBuilder.build(query);
        if (queries.isEmpty()) {
            log.info("Unsplash: sorğu qurula bilmədi (destination={})", query.destination());
            return List.of();
        }

        // Tələb: sorğu zənciri hər destination üçün logda görünməlidir.
        log.info("Unsplash sorğu zənciri (destination={}): {}", query.destination(), queries);

        for (String queryText : queries) {
            List<String> queryTerms = Arrays.asList(queryText.split("\\s+"));

            List<UnsplashPhotoCandidate> parsed = searchOnce(queryText);
            List<UnsplashPhotoCandidate> relevant =
                    ranker.rankRelevant(parsed, query, queryTerms);

            if (!relevant.isEmpty()) {
                List<ImageCandidateResponse> candidates = toResponses(relevant);
                log.info("Unsplash: '{}' -> '{}' (destination={}, namizəd={}/{} nəticə)",
                        query.destination(), queryText, query.destination(),
                        candidates.size(), parsed.size());
                return candidates;
            }
            log.debug("Unsplash: '{}' sorğusu uyğun şəkil vermədi (destination={})",
                    queryText, query.destination());
        }

        log.info("Unsplash: '{}' üçün istifadə oluna bilən şəkil tapılmadı ({} sorğu)",
                query.destination(), queries.size());
        return List.of();
    }

    /**
     * Bir sorğu. Hər növ xəta (timeout, 403 limit, 5xx, bozuk JSON) boş siyahı
     * qaytarır — çağıran tərəf növbəti yarımçıq qalan deyil, növbətin sonrakı
     * sorğusuna keçir.
     */
    private List<UnsplashPhotoCandidate> searchOnce(String query) {
        try {
            String response = webClient.get()
                    .uri(builder -> searchUri(builder, query))
                    // Açar hər sorğuda ayrıca qoyulur ki, konfiqurasiyada olmasa da
                    // heç bir yerdə "boş açar" göndərilməsin.
                    .header("Authorization", "Client-ID " + properties.getUnsplash().getAccessKey())
                    .retrieve()
                    // String alınır, YOX JsonNode deyil: Unsplash (və ya onun CDN/proxy
                    // qatı) content-type'ı "application/octet-stream" göndərsə,
                    // WebClient JsonNode-a avtomatik deserializasiya edə bilmir və
                    // bütün sorğu boş siyahıya düşürdü. İçindəki JSON'u özümüz oxuyuruq.
                    .bodyToMono(String.class)
                    .timeout(properties.getUnsplash().getTimeout())
                    .block();
            return parseCandidates(response);
        } catch (Exception e) {
            // Stack trace burada şəkil axını üçün şər atır — provider xətası tez-tez
            // təkrarlanır və heç vaxt planı dayandırmır. Yalnız sətri loglayırıq.
            log.warn("Unsplash sorğusu uğursuz oldu (query='{}'): {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Unsplash JSON cavabını bizim daxili modelə çevirir. Burada URL-lər və
     * attribution məlumatı ilə yanaşı alt təsvir, teqlər, yer və ölçü də
     * götürülür — ranker bunlarla işləyir.
     */
    private List<UnsplashPhotoCandidate> parseCandidates(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        JsonNode results;
        try {
            JsonNode response = objectMapper.readTree(body);
            results = response == null ? null : response.path("results");
        } catch (Exception e) {
            log.warn("Unsplash cavabı JSON deyil ({} simvol) — nəticə boş sayılır", body.length());
            return List.of();
        }
        if (results == null || !results.isArray()) {
            return List.of();
        }

        List<UnsplashPhotoCandidate> candidates = new ArrayList<>();
        for (JsonNode result : results) {
            String url = urlOfSize(result, CARD_URL_SIZE);
            if (url == null) {
                // Heç bir ölçüdə URL yoxdursa bu nəticə istifadə oluna bilməz.
                continue;
            }
            // Tam ekran üçün ayrıca URL: `full` yoxdursa `raw`, o da yoxdursa kart URL-i.
            String fullUrl = firstNonBlank(
                    urlOfSize(result, "full"),
                    urlOfSize(result, "raw"),
                    url);

            JsonNode user = result.path("user");

            candidates.add(new UnsplashPhotoCandidate(
                    result.path("id").asText(null),
                    url,
                    fullUrl,
                    describe(result),
                    result.path("width").asInt(0),
                    result.path("height").asInt(0),
                    textOrNull(user.path("name").asText(null)),
                    textOrNull(user.path("links").path("html").asText(null)),
                    textOrNull(result.path("links").path("html").asText(null))
            ));
        }
        return candidates;
    }

    /** Sıralanmış namizədlərdən API DTO-suna keçir (təkrarları silər). */
    private List<ImageCandidateResponse> toResponses(List<UnsplashPhotoCandidate> ranked) {
        List<ImageCandidateResponse> responses = new ArrayList<>();
        for (UnsplashPhotoCandidate candidate : ranked) {
            if (responses.size() >= MAX_CANDIDATES) {
                break;
            }
            String url = candidate.url();
            // Eyni foto iki dəfə gələ bilər (müxtəlif sorğu ölçüləri) — birini saxla.
            boolean duplicate = responses.stream().anyMatch(r -> r.url().equals(url));
            if (duplicate) {
                continue;
            }
            responses.add(new ImageCandidateResponse(
                    candidate.id(),
                    url,
                    firstNonBlank(candidate.fullUrl(), url),
                    candidate.photographer(),
                    candidate.photographerUrl(),
                    candidate.unsplashUrl()
            ));
        }
        return responses;
    }

    /** Foto haqqında bütün mətn sahələrini birləşdirir (axtarış üçün "söz daşyıcı"). */
    private String describe(JsonNode result) {
        StringBuilder sb = new StringBuilder();
        appendText(sb, result.path("alt_description").asText(null));
        appendText(sb, result.path("description").asText(null));

        JsonNode tags = result.path("tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                appendText(sb, tag.path("title").asText(null));
            }
        }

        JsonNode location = result.path("location");
        if (location.isObject()) {
            appendText(sb, location.path("name").asText(null));
            appendText(sb, location.path("city").asText(null));
            appendText(sb, location.path("country").asText(null));
        }
        return sb.toString().trim();
    }

    private void appendText(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(value);
    }

    /** Müəyyən ölçüdə URL-i qaytarır; yoxdursa null. */
    private String urlOfSize(JsonNode result, String size) {
        return textOrNull(result.path("urls").path(size).asText(null));
    }

    /** Bir nəticinin istifadə oluna bilən ilk URL-i (regular -> small -> full -> thumb). */
    private String firstUsableUrl(JsonNode result) {
        for (String size : URL_FALLBACK_SIZES) {
            String url = urlOfSize(result, size);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String textOrNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private URI searchUri(UriBuilder uriBuilder, String query) {
        return uriBuilder
                .path(properties.getUnsplash().getSearchPath())
                .queryParam("query", query)
                .queryParam("per_page", properties.getUnsplash().getPerPage())
                .queryParam("orientation", properties.getUnsplash().getOrientation())
                .queryParam("content_filter", properties.getUnsplash().getContentFilter())
                .build();
    }

    // ------------------------------------------------------------------
    // Redis keşi — mövcud HomepageStatsService üslubunda: uğursuzluq halında
    // səssizcə keçilir, heç vaxt plan generasiyasını dayandırmır.
    //
    // Keçmişdə TƏK URL saxlanırdı. İndi bütün namizəd siyahısı saxlanılır ki,
    // (a) frontend fərqli şəkil seçə bilsin, (b) səhv seçim bir ay boyu qalmasın.
    // ------------------------------------------------------------------

    private List<ImageCandidateResponse> readFromCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            String raw = objectMapper.convertValue(value, String.class);
            if (raw == null || raw.isBlank()) {
                // Boş sətir = keşdə "tapılmadı" qeydi var.
                return List.of();
            }
            return objectMapper.readValue(raw, new TypeReference<List<ImageCandidateResponse>>() {});
        } catch (Exception e) {
            // Köhnə v1 dəyərləri (tək URL) və ya bozuk JSON: səssizcə keçib
            // yenidən axtarışa gedirik — heç bir istisna çağıran tərəfə çatmır.
            log.warn("Şəkil keşi oxunmadı, Unsplash-a gediləcək (key={}): {}", key, e.getMessage());
            return null;
        }
    }

    private void writeToCache(String key, List<ImageCandidateResponse> candidates, boolean negative) {
        try {
            Duration ttl = negative
                    ? properties.getUnsplash().getNegativeCacheTtl()
                    : properties.getUnsplash().getCacheTtl();
            String value = candidates.isEmpty()
                    ? NOT_FOUND_MARKER
                    : objectMapper.writeValueAsString(candidates);
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Şəkil keşi yazılmadı (key={}): {}", key, e.getMessage());
        }
    }

    /**
     * Redis açarı: "Dolomites" + "Italy" -> "dolomites-italy" -> "image:v2:dolomites-italy".
     * Sadəcə Latin hərfləri və rəqəmlər qalır, kiçik hərfə salınır.
     */
    private String cacheSlug(String destination, String country) {
        String slug = slugify(destination);
        String cleanCountry = clean(country);
        if (cleanCountry != null && !cleanCountry.equalsIgnoreCase(destination)) {
            slug = slug + "-" + slugify(cleanCountry);
        }
        return slug;
    }

    private String slugify(String value) {
        if (value == null) {
            return "destination";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "destination" : normalized;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}