package com.voyanta.image;

import com.voyanta.survey.enums.InterestType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Destinasiya məlumatından Unsplash sorğuları zənciri qurur.
 *
 * <p>Sorğular ən konkretdən ən ümumiyə doğru sıralanır. Hər sorğu özündə
 * <b>məqsədli</b> məlumat daşıyır — heç bir vaxt kor-koranə "travel" və ya
 * "beautiful place" kimi ümumi söz əlavə olunmur.
 *
 * <p>Nümunələr (loglarda görünür):
 * <pre>
 *   Misir + "Tarix və mədəniyyət" -> "Giza pyramids Egypt"
 *   İsmayıllı + "Təbiət"         -> "Ismayilli Azerbaijan mountains nature landscape"
 *   Baku + Azərbaycan           -> "Baku Azerbaijan landmarks"
 *   Paris + Fransa               -> "Eiffel Tower Paris France"
 *   Maldives                     -> "Maldives tropical island beach coast"
 *   Iceland                      -> "Iceland landscape nature"
 *   Greenland                    -> "Greenland Arctic landscape"
 * </pre>
 *
 * <p>Yenipea destination əlavə etmək üçün <b>kod dəyişməz</b>: ad Azərbaycanca
 * yazılsa belə transliterasiya + kateqoriya birləşməsi işə salır. Yalnız
 * məşhur abidələr üçün {@link DestinationLexicon} sətir əlavə etmək kifayətdir.
 */
@Component
public class DestinationImageQueryBuilder {

    /**
     * Verilmiş kontekst üçün sınama sırası olan sorğuları qaytarır.
     * Boş olmağa bilər (ad tamamilə boşdursa).
     */
    public List<String> build(DestinationImageQuery query) {
        if (query == null) {
            return List.of();
        }

        String place = DestinationLexicon.canonicalPlace(query.destination());
        String country = DestinationLexicon.canonicalCountry(query.country());
        if (place == null && country == null) {
            return List.of();
        }

        // Ad == ölkə ("Misir"/"Misir") olduqda təkrar etmək axtarışı zəiflədir.
        boolean sameAsCountry = place != null && country != null
                && DestinationLexicon.normalizeKey(place)
                .equals(DestinationLexicon.normalizeKey(country));
        String effectiveCountry = sameAsCountry ? null : country;

        InterestType category = DestinationLexicon.categoryOf(query);
        String categoryTerms = DestinationLexicon.categoryTerms(category);
        String landmark = DestinationLexicon.landmarkFor(place, country);
        String geoHint = DestinationLexicon.geoHintFor(place, effectiveCountry);

        Set<String> queries = new LinkedHashSet<>();

        // 1) Ən konkret: abidə + yer. Məs. "Giza pyramids Egypt".
        if (landmark != null) {
            queries.add(landmark);
        }

        // 2) Yer + ölkə + kateqoriya. Məs. "Ismayilli Azerbaijan mountains nature".
        if (place != null && effectiveCountry != null && categoryTerms != null) {
            queries.add(place + " " + effectiveCountry + " " + categoryTerms);
        }

        // 3) Yer + kateqoriya (ölkə yoxdursa və ya eynidirsə).
        if (place != null && categoryTerms != null) {
            queries.add(place + " " + categoryTerms);
        }

        // 4) Yer + coğrafi xüsusiyyət. Məs. "Greenland Arctic landscape".
        if (place != null && geoHint != null) {
            queries.add(place + " " + geoHint);
        }

        // 5) Yer + ölkə. Məs. "Baku Azerbaijan landmarks" (landmark yoxdursa).
        if (place != null && effectiveCountry != null) {
            queries.add(place + " " + effectiveCountry);
        }

        // 6) Yalnız ölkə — kiçik/naməlum yerlər üçün ən yaxşı nümayən.
        if (effectiveCountry != null && categoryTerms != null) {
            queries.add(effectiveCountry + " " + categoryTerms);
        }

        // 7) Yalnız yer.
        if (place != null) {
            queries.add(place);
        }

        // 8) Son cəhd: yalnız ölkə.
        if (effectiveCountry != null) {
            queries.add(effectiveCountry);
        }

        return new ArrayList<>(queries);
    }
}