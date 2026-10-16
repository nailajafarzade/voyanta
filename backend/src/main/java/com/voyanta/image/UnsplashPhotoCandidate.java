package com.voyanta.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unsplash cavabındakı bir fotonun bizim üçün vacib olan hissəsi.
 *
 * <p>Əvvəl kod yalnız {@code urls.regular} götürürdü və ilk nəticəni seçirdi.
 * Lakin Unsplash hər foto üçün <b>zəngin metadata</b> qaytarır: alt təsvir,
 * təsvir, teqlər, coğrafi yer, ölçülər, müəllif. Bu məlumat olmadan
 * "bu şəkil həqiqətən Mısır ilə bağlıdır?" sualına cavab verə bilmirdik.
 *
 * <p><b>fullUrl</b> ayrıca saxlanılır: tam ekran baxış üçün kart URL-i
 * ({@code regular}, ~1080px) çox kiçik qalır.
 *
 * @param id              Unsplash foto ID-si
 * @param url             kart üçün optimallaşdırılmış URL
 * @param fullUrl         tam ekran üçün böyük URL
 * @param description     axtarış üçün birləşdirilmiş mətn (alt təsvir + teqlər + yer)
 * @param width           şəklin eni
 * @param height          şəklin hündürlüyü
 * @param photographer    foto müəllifinin adı
 * @param photographerUrl müəllifin Unsplash profil səhifəsi
 * @param unsplashUrl     foto nun Unsplash səhifəsi
 */
public record UnsplashPhotoCandidate(
        String id,
        String url,
        String fullUrl,
        String description,
        int width,
        int height,
        String photographer,
        String photographerUrl,
        String unsplashUrl
) {

    /**
     * Kovada bu foto səhifə üçün vizual uyğunluqdur mu?
     * Çox dar/yüksək və ya çox kiçik şəkillər kart üçün işləmir.
     */
    public boolean isVisuallySuitable() {
        if (width <= 0 || height <= 0) {
            return true; // ölçü yoxdursa mübahisə etmərik, axtarışa burax
        }
        double ratio = (double) width / height;
        if (ratio < 1.0 || ratio > 3.5) {
            return false;
        }
        return width >= 640 && height >= 400;
    }

    /** Description mətnini kiçik hərfli tokenlərə bölmək üçün köməkçi. */
    public List<String> tokens() {
        List<String> tokens = new ArrayList<>();
        if (description == null) {
            return tokens;
        }
        for (String raw : description.toLowerCase(Locale.ROOT).split("[^a-z0-9çğıöşüəı]+")) {
            if (raw.length() > 2) {
                tokens.add(raw);
            }
        }
        return tokens;
    }
}