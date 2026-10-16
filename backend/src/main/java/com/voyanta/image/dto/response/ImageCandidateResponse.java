package com.voyanta.image.dto.response;

/**
 * Response DTO for a single destination photo candidate.
 *
 * <p><b>Niyəbirdən?</b> Əvvəl backend hərraq üçün TƏK bir URL qaytarırdı. Bu,
 * eynihammerəkxeyni göstəriləndə təkrar şəklin qarşısını alırdı və frontend-ə
 * seçim etmək üçün heç nə vermirdi. İndi bir destinationsa birdən çox <b>relevant</b>
 * namizəd qaytarılır və frontend hər kart üçün fərqli şəkil seçə bilər.
 *
 * <p><b>Vacib:</b> bu DTO heç vaxt API açarı daşımır. `accessKey` yalnız
 * backend-də qalır; frontend yalnız bu URL-ləri görür.
 *
 * @param id              Unsplash foto ID-si (təkrar nəticiləri ayırmaq üçün)
 * @param url             kart üçün optimallaşdırılmış URL (kart ölçüsünə uyğun)
 * @param fullUrl         daha böyük URL — tam ekran baxış (lightbox) üçün
 * @param photographer    foto müəllifinin adı (attribution üçün)
 * @param photographerUrl müəllifin Unsplash profil səhifəsi
 * @param unsplashUrl     foto nun Unsplash səhifəsi (keçid/attribution)
 */
public record ImageCandidateResponse(
        String id,
        String url,
        String fullUrl,
        String photographer,
        String photographerUrl,
        String unsplashUrl
) {
}