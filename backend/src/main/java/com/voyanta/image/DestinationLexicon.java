package com.voyanta.image;

import com.voyanta.survey.enums.InterestType;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Destinasiya adlarını axtarış üçün "ağıllı" sözlərə çevirən məlumat lüğəti.
 *
 * <b>Niyə bu fayl lazımdır?</b> Voyanta-nın DB-dəki konglomerat adları AZƏRBAYCAN
 * dilində saxlanılır ("Misir", "İsmayıllı", "Azərbaycan", "Maldiv adaları").
 * Unsplash isə ingilis dilli bir axtarış indeksi — "Misir Misir travel landscape"
 * sorğusu onu <b>dəniz</b> şəklinə yönləndirir, çünki heç bir token uyğun gəlmir.
 *
 * <p>Bu, problemdir — əsl səbəb "travel landscape" sözü deyil, <b>adın özüdür</b>.
 *
 * <p>Üç qatlı həll (sırayla sınanır):
 * <ol>
 *   <li><b>Lüğət</b> — bilinen ad/ölkə/lorientasiya ələlə (data, kod deyil);</li>
 *   <li><b>Transliterasiya</b> — lüğətdə olmayan hər azərbaycanca ad üçün
 *       ş→sh, ç→ch, ğ→gh, ı→i, ə→e... çevrilir. Bu, "İsmayıllı" → "Ismayilli"
 *       kimi <b>ümumi</b> qaydadır: yeni ad əlavə etsəniz, kod dəyişməz;</li>
 *   <li><b>Heç nə</b> — ad onsuz da latındırsa dəyişmir.</li>
 * </ol>
 *
 * <p>Heç bir konkretSorğu burada <b>hardcode edilmir</b>: hər üç qat da
 * {@code buildQueries} tərəfindən eyni ardıcıllıqla işlədilir.
 */
public final class DestinationLexicon {

    // ------------------------------------------------------------------
    // 1) Azərbaycanca / qeyri-ingilis adlar -> ingilis kanonik adı.
    //    Açar: kiçik hərfli, akscent-siz (lookup zamanı da belə hesablanır).
    // ------------------------------------------------------------------
    private static final Map<String, String> PLACE_ALIASES = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // 2) Ölkə adları (məs. "Azərbaycan" -> "Azerbaijan"). Sorğunu dəqiqləşdirmək
    //    üçün istifadə olunur; destinations.cards üçün ən vacib siyasi məna daşıyır.
    // ------------------------------------------------------------------
    private static final Map<String, String> COUNTRY_ALIASES = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // 3) Məşhur abidə / görünüş açar sözləri. Açar: kanonik ad.
    //    "Egipt" üçün "Giza pyramids" -> axtarış düzgün nəticə verir.
    //    BÜYÜK SİRƏT YOXDUR: bu sadəcə yaxşı məlum abidələr, yeni ölkə
    //    əlavə etmək üçün sətir yazmaq kifayətdir.
    // ------------------------------------------------------------------
    private static final Map<String, String> LANDMARKS = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // 4) Maraq/etiket -> axtarış sözləri. "Təbiət" üçün "mountains nature",
    //    "Dəniz və gündoğuş" üçün "tropical island beach" və s.
    // ------------------------------------------------------------------
    private static final Map<InterestType, String> CATEGORY_TERMS = Map.of(
            InterestType.NATURE, "mountains nature landscape",
            InterestType.SEA, "tropical island beach coast",
            InterestType.HISTORY_CULTURE, "ancient landmark historic architecture",
            InterestType.NIGHTLIFE, "city night life"
    );

    // DB-dəki `tag` sütunu AZƏRBAYCANCA sətirdir. İnterest teqləri olmayan
    // sətrdə bu sətir yeganə kateqoriya mənbəyimizdir.
    private static final Map<String, InterestType> TAG_HINTS = Map.ofEntries(
            Map.entry("təbiət", InterestType.NATURE),
            Map.entry("tebiyet", InterestType.NATURE),
            Map.entry("nature", InterestType.NATURE),
            Map.entry("dəniz", InterestType.SEA),
            Map.entry("deniz", InterestType.SEA),
            Map.entry("sea", InterestType.SEA),
            Map.entry("gündoğuş", InterestType.SEA),
            Map.entry("tarix və mədəniyyət", InterestType.HISTORY_CULTURE),
            Map.entry("tarix", InterestType.HISTORY_CULTURE),
            Map.entry("mədəniyyət", InterestType.HISTORY_CULTURE),
            Map.entry("gecə həyatı", InterestType.NIGHTLIFE),
            Map.entry("gece hayati", InterestType.NIGHTLIFE),
            Map.entry("nightlife", InterestType.NIGHTLIFE)
    );

    // Coğrafi xüsusiyyət sözü: "Greenland" üçün "Arctic landscape" kimi.
    // Bu, "quru hava / şəhər" kimi yanlış nəticələri nötrallaşdırır.
    private static final Map<String, String> GEO_HINTS = new LinkedHashMap<>();

    static {
        geoHint("greenland", "Arctic landscape");
        geoHint("qrinlandiya", "Arctic landscape");
        geoHint("iceland", "landscape nature");
        geoHint("islandiya", "landscape nature");
        geoHint("norway", "fjord mountains");
        geoHint("norveç", "fjord mountains");
        geoHint("switzerland", "alps mountains");
        geoHint("isveçrə", "alps mountains");
        geoHint("nepal", "himalaya mountains");
        geoHint("patagonia", "mountains glacier");
        geoHint("faroe islands", "cliff coastline");
        geoHint("svalbard", "arctic glacier");
        geoHint("azerbaijan", "mountains nature landscape");
        geoHint("azərbaycan", "mountains nature landscape");

        place("misir", "Egypt");
        place("misir memləketi", "Egypt");
        place("qədim mismir", "Ancient Egypt");
        place("giza", "Giza");
        place("gizah", "Giza");
        place("luxor", "Luxor");
        place("asvan", "Aswan");
        place("santorini", "Santorini");
        place("maldiv adaları", "Maldives");
        place("maldiv adalari", "Maldives");
        place("maldivlər", "Maldives");
        place("maldives", "Maldives");
        place("baku", "Baku");
        place("baku şəhəri", "Baku");
        place("ismayilli", "Ismayilli");
        place("gəncə", "Ganja");
        place("gancə", "Ganja");
        place("quba", "Quba");
        place("şəki", "Sheki");
        place("sheki", "Sheki");
        place("lankaran", "Lankaran");
        place("dənizkəndi", "Masallı");
        place("xəzər", "Caspian Sea");
        place("paris", "Paris");
        place("london", "London");
        place("rome", "Rome");
        place("roma", "Rome");
        place("barcelona", "Barcelona");
        place("madrid", "Madrid");
        place("amsterdam", "Amsterdam");
        place("berlin", "Berlin");
        place("venice", "Venice");
        place("venedik", "Venice");
        place("florence", "Florence");
        place("prague", "Prague");
        place("praha", "Prague");
        place("vienna", "Vienna");
        place("budapest", "Budapest");
        place("athens", "Athens");
        place("afina", "Athens");
        place("istanbul", "Istanbul");
        place("kapadokya", "Cappadocia");
        place("cappadocia", "Cappadocia");
        place("tokyo", "Tokyo");
        place("kyoto", "Kyoto");
        place("seoul", "Seoul");
        place("suriya", "Vietnam");
        place("vyetnam", "Vietnam");
        place("tailand", "Thailand");
        place("bangkok", "Bangkok");
        place("hindistan", "India");
        place("delhi", "Delhi");
        place("jaipur", "Jaipur");
        place("nepal", "Nepal");
        place("peru", "Peru");
        place("məşhuş", "Machu Picchu");
        place("machu picchu", "Machu Picchu");
        place("meksika", "Mexico");
        place("meksiko", "Mexico");
        place("braziliya", "Brazil");
        place("rio de janeiro", "Rio de Janeiro");
        place("argentina", "Argentina");
        place("buenos aires", "Buenos Aires");
        place("çin", "China");
        place("cin", "China");
        place("pekin", "Beijing");
        place("şanghai", "Shanghai");
        place("yaponiya", "Japan");
        place("seul", "Seoul");
        place("avstraliya", "Australia");
        place("sydney", "Sydney");
        place("melburn", "Melbourne");
        place("yeni zelandiya", "New Zealand");
        place("kaqaret", "Cape Town");
        place("keyp taun", "Cape Town");

        country("azərbaycan", "Azerbaijan");
        country("azerbaycan", "Azerbaijan");
        country("azərbaycan respublikası", "Azerbaijan");
        country("misir", "Egypt");
        country("yunanıstan", "Greece");
        country("yunanistan", "Greece");
        country("maldivlər", "Maldives");
        country("türkiyə", "Turkey");
        country("turkiye", "Turkey");
        country("gürcüstan", "Georgia");
        country("qırğızıstan", "Kyrgyzstan");
        country("özbəkistan", "Uzbekistan");
        country("qazaxıstan", "Kazakhstan");
        country("italiya", "Italy");
        country("fransa", "France");
        country("i̇spaniya", "Spain");
        country("ispaniya", "Spain");
        country("portuqaliya", "Portugal");
        country("britaniya", "United Kingdom");
        country("ingiltərə", "United Kingdom");
        country("almaniya", "Germany");
        country("belçika", "Belgium");
        country("isveçrə", "Switzerland");
        country("avstriya", "Austria");
        country("polşa", "Poland");
        country("macarıstan", "Hungary");
        country("rumıniya", "Romania");
        country("yunanıstan və roma", "Italy");
        country("qara dəniz", "Black Sea");
        country("qərbi avropa", "Europe");
        country("şərqi avropa", "Europe");
        country("cənub-şərqi asiya", "Asia");
        country("norveç", "Norway");
        country("isveç", "Sweden");
        country("danimarka", "Denmark");
        country("finlandiya", "Finland");
        country("islandiya", "Iceland");
        country("qrinlandiya", "Greenland");
        country("estoniya", "Estonia");
        country("latviya", "Latvia");
        country("litva", "Lithuania");
        country("çexiya", "Czechia");
        country("slovakiya", "Slovakia");
        country("xorvatiya", "Croatia");
        country("sloveniya", "Slovenia");
        country("serbiya", "Serbia");
        country("yunaniya", "Greece");
        country("hollandiya", "Netherlands");
        country("irlandiya", "Ireland");
        country("kanada", "Canada");
        country("meksika", "Mexico");
        country("braziliya", "Brazil");
        country("argentina", "Argentina");
        country("çili", "Chile");
        country("peru", "Peru");
        country("kolumbiya", "Colombia");
        country("avstraliya", "Australia");
        country("yeni zelandiya", "New Zealand");
        country("hindistan", "India");
        country("çin", "China");
        country("yaponiya", "Japan");
        country("cənub-şərqi asiya", "Southeast Asia");
        country("yaxşı şərq", "Middle East");
        country("şimali afrika", "North Africa");
        country("mərkəzi asiya", "Central Asia");
        country("qafqaz", "Caucasus");
        country("şimali qafqaz", "North Caucasus");
        country("qeddes", "Jordan");
        country("iordaniya", "Jordan");
        country("bəhrəyn", "Bahrain");
        country("qatar", "Qatar");
        country("səudiyyə", "Saudi Arabia");
        country("birləşmiş ərəblər", "United Arab Emirates");
        country("dubay", "Dubai");
        country("tayland", "Thailand");
        country("vyetnam", "Vietnam");
        country("kamboca", "Cambodia");
        country("indoneziya", "Indonesia");
        country("malaysia", "Malaysia");
        country("seysellər", "Seychelles");
        country("mauritius", "Mauritius");
        country("madaqaskar", "Madagascar");
        country("moritaniya", "Mauritania");
        country("tunis", "Tunisia");
        country("mərakeş", "Morocco");
        country("əl cəbir", "United Arab Emirates");
        country("müşirət", "Oman");
        country("yəmən", "Yemen");
        country("irak", "Iraq");
        country("iran", "Iran");
        country("suriya", "Syria");
        country("livan", "Lebanon");
        country("israil", "Israel");
        country("güney afrika", "South Africa");
        country("kenya", "Kenya");
        country("tanzaniya", "Tanzania");
        country("ruanda", "Rwanda");
        country("zambiya", "Zambia");
        country("sudan", "Sudan");
        country("ətiopiya", "Ethiopia");
        country("mərakeş", "Morocco");
        country("moldova", "Moldova");
        country("belorussiya", "Belarus");
        country("ukrayna", "Ukraine");
        country("rusiya", "Russia");

        landmark("egypt", "Giza pyramids Egypt");
        landmark("giza", "Giza pyramids Egypt");
        landmark("ancient egypt", "pyramids temples Egypt");
        landmark("luxor", "Luxor temple valley of the kings");
        landmark("aswan", "Aswan Nile river");
        landmark("santorini", "Santorini Oia Greece");
        landmark("greece", "Acropolis Greece");
        landmark("athens", "Acropolis Athens");
        landmark("rome", "Colosseum Rome Italy");
        landmark("florence", "Florence Duomo Italy");
        landmark("venice", "Venice Grand Canal Italy");
        landmark("barcelona", "Sagrada Familia Barcelona");
        landmark("madrid", "Prado Museum Madrid");
        landmark("london", "Big Ben Tower Bridge London");
        landmark("paris", "Eiffel Tower Paris France");
        landmark("amsterdam", "Amsterdam canals Netherlands");
        landmark("berlin", "Brandenburg Gate Berlin");
        landmark("prague", "Prague Castle Czechia");
        landmark("vienna", "Schonbrunn Vienna");
        landmark("budapest", "Budapest parliament");
        landmark("moskva", "Saint Basil Moscow");
        landmark("istanbul", "Hagia Sophia Istanbul");
        landmark("cappadocia", "Cappadocia hot air balloons Turkey");
        landmark("tokyo", "Tokyo Japan");
        landmark("kyoto", "Kyoto temples Japan");
        landmark("seoul", "Seoul Gyeongbokgung South Korea");
        landmark("beijing", "Great Wall of China");
        landmark("shanghai", "Shanghai skyline China");
        landmark("taj mahal", "Taj Mahal India");
        landmark("delhi", "Delhi India monuments");
        landmark("jaipur", "Amber Fort Jaipur India");
        landmark("new york", "Manhattan skyline New York");
        landmark("los angeles", "Hollywood Los Angeles");
        landmark("san francisco", "Golden Gate Bridge San Francisco");
        landmark("sydney", "Sydney Opera House Australia");
        landmark("melbourne", "Melbourne Australia");
        landmark("rio de janeiro", "Christ the Redeemer Rio de Janeiro");
        landmark("machu picchu", "Machu Picchu Peru");
        landmark("sydney opera house", "Sydney Opera House Australia");
        landmark("tacoma", "Mount Rainier Washington");
        landmark("baku", "Baku Azerbaijan landmarks");
        landmark("sheki", "Sheki Azerbaijan old city");
        landmark("lankaran", "Lankaran Azerbaijan");
        landmark("dubai", "Burj Khalifa Dubai");
        landmark("doha", "Doha skyline Qatar");
        landmark("singapore", "Marina Bay Singapore");
        landmark("hong kong", "Hong Kong skyline");
        landmark("bangkok", "Bangkok Thailand temple");
        landmark("phuket", "Phuket Thailand beach");
        landmark("bali", "Bali rice terraces Indonesia");
        landmark("machu", "Machu Picchu Peru");
    }

    private DestinationLexicon() {
    }

    /**
     * Açarı normalize edərək saxlayır. Bu vacibdir: `normalizeKey` aksentləri
     * təmizləyir ("Maldivlər" -> "maldivler"), ona görə lüğət açarı da eyni
     * qaydayla saxlanılmalıdır — yoxsa heç bir lüğət sətri heç vaxt tapılmaz.
     */
    private static void place(String alias, String canonical) {
        String key = normalizeKey(alias);
        if (key != null) {
            PLACE_ALIASES.put(key, canonical);
        }
    }

    private static void country(String alias, String canonical) {
        String key = normalizeKey(alias);
        if (key != null) {
            COUNTRY_ALIASES.put(key, canonical);
        }
    }

    private static void landmark(String key, String terms) {
        String normalized = normalizeKey(key);
        if (normalized != null) {
            LANDMARKS.put(normalized, terms);
        }
    }

    private static void geoHint(String key, String terms) {
        String normalized = normalizeKey(key);
        if (normalized != null) {
            GEO_HINTS.put(normalized, terms);
        }
    }

    /**
     * Adı ingilis kanonik formaya gətirir: əvvəlcə lüğət, sonra transliterasiya.
     * Heç bir halda istisna atmır.
     */
    public static String canonicalPlace(String raw) {
        String key = normalizeKey(raw);
        if (key == null) {
            return null;
        }
        String alias = PLACE_ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        return transliterate(raw);
    }

    /** Ölkəni kanonik ingilis adına gətirir (lüğətdə yoxdursa transliterasiya). */
    public static String canonicalCountry(String raw) {
        String key = normalizeKey(raw);
        if (key == null) {
            return null;
        }
        String alias = COUNTRY_ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        return transliterate(raw);
    }

    /**
     * Kanonik ada görə abidə/vizual açar sözləri.
     * Məs. "Egypt" -> "Giza pyramids Egypt". Tapılmasa null.
     */
    public static String landmarkFor(String canonicalPlace, String canonicalCountry) {
        String place = normalizeKey(canonicalPlace);
        if (place != null) {
            String hit = LANDMARKS.get(place);
            if (hit != null) {
                return hit;
            }
        }
        String country = normalizeKey(canonicalCountry);
        if (country != null) {
            String hit = LANDMARKS.get(country);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /** Coğrafi xüsusiyyət sözü ("Greenland" -> "Arctic landscape"), yoxdursa null. */
    public static String geoHintFor(String canonicalPlace, String canonicalCountry) {
        String place = normalizeKey(canonicalPlace);
        if (place != null && GEO_HINTS.containsKey(place)) {
            return GEO_HINTS.get(place);
        }
        String country = normalizeKey(canonicalCountry);
        if (country != null && GEO_HINTS.containsKey(country)) {
            return GEO_HINTS.get(country);
        }
        return null;
    }

    /**
     * Destinasiyanın kateqoriyasını müəyyən edir: əvvəlcə strukturlaşdırılmış
     * {@link InterestType}, yoxdursa DB-dəki azərbaycanca {@code tag} sətri.
     */
    public static InterestType categoryOf(DestinationImageQuery query) {
        if (query == null) {
            return null;
        }
        Set<InterestType> interests = query.interests();
        if (interests != null && !interests.isEmpty()) {
            // Sabit sıra ilə: NATURE/SEA/HISTORY qarışıq olduqda təsadüfi seçim olmasın.
            for (InterestType candidate : List.of(InterestType.NATURE, InterestType.SEA,
                    InterestType.HISTORY_CULTURE, InterestType.NIGHTLIFE)) {
                if (interests.contains(candidate)) {
                    return candidate;
                }
            }
        }
        return categoryFromTag(query.tag());
    }

    /** Azərbaycanca/ingilisca tag sətrindən kateqoriya çıxarır, tapmasa null. */
    public static InterestType categoryFromTag(String tag) {
        String key = normalizeKey(tag);
        if (key == null) {
            return null;
        }
        // "tarix və mədəniyyət" əvvəlcə yoxlanılır ki, "mədəniyyət" qısaltması
        // daha ümumi sətirlə toqquşmasın.
        for (Map.Entry<String, InterestType> entry : TAG_HINTS.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Kateqoriya üçün axtarış sözləri (NATURE -> "mountains nature landscape"). */
    public static String categoryTerms(InterestType category) {
        return category == null ? null : CATEGORY_TERMS.get(category);
    }

    // ------------------------------------------------------------------
    // Açar hesablama + transliterasiya
    // ------------------------------------------------------------------

    /** Böyük/kiçik hərf, akscent və boşluqdan təmizlənmiş lüğət açarı. */
    static String normalizeKey(String raw) {
        if (raw == null) {
            return null;
        }
        String ascii = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9çğıöşüəı]+", " ")
                .trim();
        // Azərbaycan hərflərini də ASCII-yə yaxınlaşdır ki, açar uyğun gəlsin.
        ascii = ascii
                .replace("ç", "c").replace("ğ", "g").replace("ö", "o")
                .replace("ş", "s").replace("ü", "u").replace("ə", "e")
                .replace("ı", "i");
        ascii = ascii.replaceAll("\\s+", " ").trim();
        return ascii.isEmpty() ? null : ascii;
    }

    /**
     * Azərbaycanca hərfləri ingilis yazışına çevirir. Heç bir lüğət qaydası
     * işləməsə belə adı axtarışa yararlı edir ("İsmayıllı" -> "Ismayilli").
     *
     * <p><b>Hərflərin böyüklüyü qorunur.</b> Əgər ad kiçik hərfə salınsaydı,
     * "Faroe Islands" -> "faroe islands" olardı: axtarış nəticəsi eyni qalır,
     * amma loglar oxunaq bilmirdi və çoxsözly adlar ("United Arab Emirates")
     * qarışırdı. Yalnız hərflər <i>çevrilir</i>, hərflər böyüklüyü isə saxlanılır.
     */
    static String transliterate(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replaceAll("\\s+", " ");
        if (text.isEmpty()) {
            return null;
        }
        // "İ" -> "I" üçün: NFD + aksent silinməsi bütövlükdə "İ" -> "I" verir.
        String result = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return result
                .replace("Ç", "Ch").replace("ç", "ch")
                .replace("Ğ", "Gh").replace("ğ", "gh")
                .replace("Ş", "Sh").replace("ş", "sh")
                .replace("Ö", "O").replace("ö", "o")
                .replace("Ü", "U").replace("ü", "u")
                .replace("Ə", "E").replace("ə", "e")
                .replace("ı", "i");
    }
}