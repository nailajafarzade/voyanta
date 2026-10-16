import { normaliseImageUrl, toImageCandidates } from "./imageCandidates";


/**
 * Backend `Destination` modelu — bütün endpoint-lər (featured, personalized, wishlist)
 * bu formayı qaytarır və frontend-də TƏK bu tipdən istifadə olunur.
 *
 * @typedef {Object} Destination
 * @property {string} id
 * @property {string} name
 * @property {string} country
 * @property {string|null} imageUrl  Backend tərəfdən həll olunur (Unsplash + keş).
 *                                    AI planları üçün də həmin backend axtarışından gəlir,
 *                                    ona görə istiqamət əvvəlcədən təyin edilməmiş ola bilər.
 * @property {string|null} tag
 */

/**
 * Destination → dizaynın kart formatı { id, title, subtitle, imageUrl }.
 * Beləliklə kartların markup-ı dəyişmir.
 *
 * @param {Destination} destination
 * @returns {{ id: string, title: string, subtitle: string, imageUrl: string|null }}
 */
export function toPlace(destination) {
  // Backend-in `images[]` namizədləri KORUNUR — eyni istiqamət
  // Popular / "Sənə xüsusi" bölmələrində FƏRQli şəkil göstərə bilir
  // (bax: utils/imageAssignment.js). Sıra backend-dən gəlir, POZULMUR.
  const images = toImageCandidates(destination);

  return {
    id: destination.id,
    title: `${destination.name}, ${destination.country}`,
    subtitle: destination.tag,
    // images[0] — backend-in ən çox uyğun namizədi (relevans qaydası).
    // Namizəd yoxdursa mövcud `imageUrl` işə düşür, o da yoxdursa universal
    // fallback (DestinationImage) göstərilir. Heç vaxt saxta URL yaratmır.
    imageUrl: images[0]?.url ?? normaliseImageUrl(destination.imageUrl),
    images,
  };
}

/**
 * Backend `imageUrl`-unu təmizləyir. Burda HEÇ BİR istiqamət → şəkil seçimi yoxdur:
 * frontend yalnız gələn dəyəri ötürür, boş dəyəri null-a çevirir.
 * @param {unknown} value
 * @returns {string|null}
 */
export { normaliseImageUrl };

