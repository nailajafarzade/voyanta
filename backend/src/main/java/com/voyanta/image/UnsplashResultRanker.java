package com.voyanta.image;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Unsplash nəticələrini <b>semantik olaraq sıralayır</b> və relevant olanlar
 * arasından təsadüfi birini seçir.
 *
 * <p>Əvvəlki davranış: {@code results[0]}. Bu, sorğu düzgün olsa belə
 * (məs. "Giza pyramids Egypt") 3-cü nəticidə uzaq semada olan şəkil ola bilər.
 *
 * <p>İndi: hər nəticə Ünsplash-ın öz metadata-sı (alt təsvir, teqlər, yer)
 * ilə qiymətləndirilir:
 * <ol>
 *   <li><b>Dəstinqlər</b> (destination tokens) — ən ağır çəki;</li>
 *   <li><b>Abidə sözü</b> — "pyramid", "eiffel" kimi;</li>
 *   <li><b>Ölkə</b>;</li>
 *   <li><b>Kateqoriya</b> (təbiət/dəniz/tarix) — aşağı çəki;</li>
 *   <li><b>Vizual uyğunluq</b> — ölçü və format.</li>
 * </ol>
 *
 * <p>Təsadüfi seçim <b>yalnız yuxarı relevanslı hədlər arasında</b> baş verir —
 * yəni çeşidlilik heç vaxt "qəribə şəkil" demək deyil. Bu, tələbin məhz nəzərdə
 * tutulan nöqtəsidir.
 */
@Component
public class UnsplashResultRanker {

    // Nəticənin ümumi relevansı bu həddin altındasa, şəkil ümumiyyətlə
    // əlaqəli OLMAMALIDIR — belə nəticə "heç nə tapılmadı" sayılır.
    private static final double MIN_ACCEPTABLE_SCORE = 1.0;

    // Təsadüfi seçim yalnız ən yaxşı nəticənin bu qədər yaxınlığındakılar arasında.
    private static final double VARIETY_TOLERANCE = 0.6;

    private static final double WEIGHT_DESTINATION = 4.0;
    private static final double WEIGHT_LANDMARK = 3.0;
    private static final double WEIGHT_COUNTRY = 2.0;
    private static final double WEIGHT_CATEGORY = 1.0;
    private static final double BONUS_VISUAL = 1.5;

    /**
     * Nəticələri qiymətləndirib <b>relevans sırası ilə</b> qaytarır.
     *
     * <p>Bu, çoxnamizədli cavabın əsas metodudur: frontend hər kart üçün
     * fərqli şəkil seçə biləcəyi halda siyahı lazımdır. Yalnız RELEVANT
     * nəticələr qaytarılır — relevance həddindən aşağı düşən şəkillər heç vaxt
     * siyahıya düşmür.
     *
     * @return ən yaxşıdan ən zəifə sıralanmış relevant namizədlər (boş ola bilər)
     */
    public List<UnsplashPhotoCandidate> rankRelevant(List<UnsplashPhotoCandidate> candidates,
                                                      DestinationImageQuery query,
                                                      List<String> queryTerms) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Set<String> destinationTokens = meaningfulTokens(
                DestinationLexicon.canonicalPlace(query.destination()),
                DestinationLexicon.canonicalCountry(query.country()));
        Set<String> landmarkTokens = meaningfulTokens(landmarkOf(queryTerms));
        Set<String> countryTokens = meaningfulTokens(DestinationLexicon.canonicalCountry(query.country()));
        Set<String> categoryTokens = meaningfulTokens(
                DestinationLexicon.categoryTerms(DestinationLexicon.categoryOf(query)));

        List<Scored> scored = new ArrayList<>();
        for (UnsplashPhotoCandidate candidate : candidates) {
            if (candidate == null || candidate.url() == null || candidate.url().isBlank()) {
                continue;
            }
            // Mətn uyğunluğu və vizual uyğunluq AYRI hesablanır: vizual bonus
            // heç vaht özü ilə bir şəkli "relevant" etməməlidir, sadəcə sıralamanı
            // yaxşılaşdırır. Əks halda Mısır üçün təsadüfi bir çimərik foto
            // "düzgün" sayılaraq qəbul edilərdi.
            double textual = textScore(candidate, destinationTokens,
                    landmarkTokens, countryTokens, categoryTokens);
            double visual = candidate.isVisuallySuitable() ? BONUS_VISUAL : 0;
            scored.add(new Scored(candidate, textual, textual + visual));
        }

        scored.sort((a, b) -> Double.compare(b.total(), a.total()));

        List<UnsplashPhotoCandidate> relevant = new ArrayList<>();
        for (Scored item : scored) {
            if (item.textual() >= MIN_ACCEPTABLE_SCORE) {
                relevant.add(item.candidate());
            }
        }
        return relevant;
    }

    /**
     * Nəticələri qiymətləndirib ən uyğun URL-i qaytarır.
     *
     * @return seçilmiş şəklin URL-i, heç bir nəticə relevant deyilsə {@code null}
     */
    public String pickBestUrl(List<UnsplashPhotoCandidate> candidates,
                              DestinationImageQuery query,
                              List<String> queryTerms) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Set<String> destinationTokens = meaningfulTokens(
                DestinationLexicon.canonicalPlace(query.destination()),
                DestinationLexicon.canonicalCountry(query.country()));
        Set<String> landmarkTokens = meaningfulTokens(landmarkOf(queryTerms));
        Set<String> countryTokens = meaningfulTokens(DestinationLexicon.canonicalCountry(query.country()));
        Set<String> categoryTokens = meaningfulTokens(
                DestinationLexicon.categoryTerms(DestinationLexicon.categoryOf(query)));

        List<Scored> scored = new ArrayList<>();
        for (UnsplashPhotoCandidate candidate : candidates) {
            if (candidate == null || candidate.url() == null || candidate.url().isBlank()) {
                continue;
            }
            // Mətn uyğunluğu və vizual uyğunluq AYRI hesablanır: vizual bonus
            // heç vaht özü ilə bir şəkli "relevant" etməməlidir, sadəcə sıralamanı
            // yaxşılaşdırır. Əks halda Mısır üçün təsadüfi bir çimərik foto
            // "düzgün" sayılaraq qəbul edilərdi.
            double textual = textScore(candidate, destinationTokens,
                    landmarkTokens, countryTokens, categoryTokens);
            double visual = candidate.isVisuallySuitable() ? BONUS_VISUAL : 0;
            scored.add(new Scored(candidate, textual, textual + visual));
        }

        if (scored.isEmpty()) {
            return null;
        }

        scored.sort((a, b) -> Double.compare(b.total(), a.total()));

        // Qəbul həddi MƏTN uyğunluğuna baxılır (vizual bonus sayılmır).
        Scored bestMatch = scored.stream()
                .filter(item -> item.textual() >= MIN_ACCEPTABLE_SCORE)
                .findFirst()
                .orElse(null);

        if (bestMatch == null) {
            // Heç bir nəticənin mətni destinationsla əlaqələndirilmir — bu,
            // "tapılmadı" deməkdir ki, növbəti (daha ümumi) sorğuya keçək.
            return null;
        }

        double best = bestMatch.total();

        // Çeşidlilik: yalnız ən yaxşıya YAXIN nəticələr arasından təsadüfi seçim.
        List<String> shortlist = new ArrayList<>();
        for (Scored item : scored) {
            if (item.textual() >= MIN_ACCEPTABLE_SCORE && item.total() >= best - VARIETY_TOLERANCE) {
                shortlist.add(item.candidate().url());
            }
        }

        if (shortlist.size() == 1) {
            return shortlist.get(0);
        }
        return shortlist.get(ThreadLocalRandom.current().nextInt(shortlist.size()));
    }

    /** Yalnız mətn (token) uyğunluğu — vizual bonus burada sayılmır. */
    private double textScore(UnsplashPhotoCandidate candidate,
                            Set<String> destinationTokens,
                            Set<String> landmarkTokens,
                            Set<String> countryTokens,
                            Set<String> categoryTokens) {
        Set<String> haystack = new LinkedHashSet<>(candidate.tokens());

        double total = 0;
        total += WEIGHT_DESTINATION * matches(haystack, destinationTokens);
        total += WEIGHT_LANDMARK * matches(haystack, landmarkTokens);
        total += WEIGHT_COUNTRY * matches(haystack, countryTokens);
        total += WEIGHT_CATEGORY * matches(haystack, categoryTokens);
        return total;
    }

    private double matches(Set<String> haystack, Set<String> wanted) {
        if (wanted.isEmpty()) {
            return 0;
        }
        long hits = wanted.stream().filter(haystack::contains).count();
        return (double) hits / wanted.size();
    }

    /**
     * Sorğu sözlərindən abidə hissəsini ayırır. Sorğu ad ilə başlayırsa
     * (məs. "Ismayilli Azerbaijan mountains nature"), o söz "abidə" sayılmır;
     * qalan hissə "görünüş açarı" rolunu oynayır.
     */
    private String landmarkOf(List<String> queryTerms) {
        if (queryTerms == null || queryTerms.isEmpty()) {
            return null;
        }
        return String.join(" ", queryTerms);
    }

    /**
     * Sözü axtarış üçün istifadə oluna bilən tokenlərə bölmək.
     * "Ismayilli" -> ["ismayilli"], "Giza pyramids" -> ["giza", "pyramids"].
     */
    private Set<String> meaningfulTokens(String... phrases) {
        Set<String> tokens = new LinkedHashSet<>();
        if (phrases == null) {
            return tokens;
        }
        for (String phrase : phrases) {
            if (phrase == null || phrase.isBlank()) {
                continue;
            }
            for (String raw : phrase.toLowerCase(Locale.ROOT).split("[^a-z0-9çğıöşüəı]+")) {
                if (raw.length() > 2) {
                    tokens.add(raw);
                }
            }
        }
        return tokens;
    }

    private record Scored(UnsplashPhotoCandidate candidate, double textual, double total) {
    }
}