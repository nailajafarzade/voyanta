package com.voyanta.image;

import com.voyanta.image.dto.response.ImageCandidateResponse;

import java.util.List;

/**
 * Destinasiya učün šəkil URL-i tapmaq barədə qruplaşdırılmış qarər.
 *
 * Bu, image modulunun yeganə giriş nöqtəsidir — plan, recommendation, wishlist və
 * destination modulları konkret təchizatçını (Unsplash) bilmir, yalnız bu interfeysi tanıyır.
 *
 * Niyə ayrıca image modulu:
 *  - AI generasiyası DİSTİNKTİVDİR nəticə verir (Greenland, Svalbard, Faroe Islands ...),
 *    yəni "hər Thickdestination üçün əvvəlcədən təyin edilmiş şəkil" yanaşması işləməz;
 *  - şəkil bir BAŞQA xarici servisdən gəlir və heç vaxt plan generasiyasını uğursuz
 *    etməməlidir.
 *
 * Söz öhdəsi: bu metod heç vaxt istisna atmağa və ya bloklayan şəkildə uğursuz olmağa
 * borclu deyil. Şəkil tapılmasa boş siyahı/null qaytarır — çağıran tərəf mövcud
 * cavabını olduğu kimi davam etdirir.
 */
public interface ImageService {

    /**
     * Verilmiş kontekstə görə <b>bir neçə relevant şəkil</b> qaytarır.
     *
     * <p>Əvvəl yalnız bir URL qaytarılırdı. Bu, eyni destinationsa bir neçə dəfə
     * göstərildikdə eyni şəkli təkrar etməyə məcbur edirdi. İndi <b>sıralanmış namizəd
     * siyahısı</b> qaytarılır: frontend hər kart üçün fərqli şəkil seçə bilər.
     *
     * <p>Siyahı RELEVANS QAYDASI ilə qurulur (relevance first, variety second):
     * yalnız mətn uyğunluğu həddi keçən şəkillər siyahıya düşür.
     *
     * @param queryyiqeddestination konteksti; null/boş ad olanda boş siyahı
     * @return ən yaxşıdan ən zəifə sıralanmış relevant namizədlər; heç nə tapılmasa boş
     */
    List<ImageCandidateResponse> resolveImageCandidates(DestinationImageQuery query);

    /**
     * Verilmiş kontekstə görə uyğun <b>tək</b> şəkil URL-i tapır (çoxnamizədli
     * cavabın birinci, ən yaxşı namizədi).
     *
     * <p>Kontekst destination adını, ölkəsini, kart etiketini və maraq teqlərini
     * daşıyır — sorğunun "ağıllı" olması bu məlumatlara görədir: təbiət üçün dağ,
     * tarix üçün abidə, dəniz üçün sahil axtarılır.
     *
     * @param queryqedestination konteksti
     * @return ən yaxşı namizədin URL-i, heç nə tapılmasa null
     */
    default String resolveImageUrl(DestinationImageQuery query) {
        return resolveImageCandidates(query).stream()
                .map(ImageCandidateResponse::url)
                .findFirst()
                .orElse(null);
    }

    /**
     * Sadəlik üçün qısa yol: yalnız ad və ölkə.
     *
     * @param destination destination adı (məs. "Greenland") — null/boş ola bilər
     * @param country     ölkə (məs. "Italy") — null/boş ola bilər
     * @return tapılmış şəklin URL-i, uyğun şəkil yoxdursa null
     */
    default String resolveImageUrl(String destination, String country) {
        return resolveImageUrl(DestinationImageQuery.of(destination, country));
    }
}