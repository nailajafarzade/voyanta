/**
 * Backend `ImageCandidateResponse` normalizasiyası.
 *
 * Backend hər istiqamət üçün RELEVANS QAYDASI ilə sıralanmış bir neçə şəkil
 * namizədi qaytarır (`DestinationResponse.images`), sırada 0 = ən yaxşıdır.
 * Frontend bu sıranı BOZMAYIR — yalnız təhlükəsiz formaya salır və eyni
 * URL-i təkrarlayan namizədləri ayırır.
 *
 * Burda heç bir istiqamət → şəkil seçimi və heç bir təsadüfi (random)
 * davranış yoxdur. Şəkilləri yalnız backend qaytarır.
 */

/** Yalnız brauzerin yükləyə biləcəyi URL-ləri qəbul edir (boz placeholder linklər keçmir). */
export function isUsableImageUrl(value) {
  return (
    typeof value === "string" && /^(https?:|data:|blob:|\/)/i.test(value.trim())
  );
}

/**
 * Boş/boz dəyəri `null`-a çevirir.
 * @param {unknown} value
 * @returns {string|null}
 */
export function normaliseImageUrl(value) {
  if (typeof value !== "string") return null;
  return value.trim() || null;
}

function cleanText(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

/**
 * Bir backend namizədini təhlükəsiz formaya salır:
 * `{ id, url, fullUrl, photographer, photographerUrl, unsplashUrl }`.
 *
 * URL yoxdursa `null` qaytarır — heç vaxt saxta URL yaratmır.
 * @param {unknown} raw
 * @returns {{id:string|null,url:string,fullUrl:string,photographer:string|null,
 *            photographerUrl:string|null,unsplashUrl:string|null}|null}
 */
export function normaliseImageCandidate(raw) {
  if (!raw || typeof raw !== "object") return null;

  const url = normaliseImageUrl(raw.url) ?? normaliseImageUrl(raw.imageUrl);
  if (!isUsableImageUrl(url)) return null;

  const fullUrl = normaliseImageUrl(raw.fullUrl);

  return {
    id: cleanText(raw.id),
    url,
    // fullUrl yoxdursa kart URL-i işlədilir — tam ekran da həmin şəkli göstərir,
    // heç bir zaman şəkil uydurulmur.
    fullUrl: isUsableImageUrl(fullUrl) ? fullUrl : url,
    photographer: cleanText(raw.photographer),
    photographerUrl: cleanText(raw.photographerUrl),
    unsplashUrl: cleanText(raw.unsplashUrl),
  };
}

/**
 * `Destination.images[]` → təhlükəsiz namizəd siyahısı (backend sırası qorunur).
 *
 * Fallback ardıcıllığı (AI plan şəkilləri üçün də eyni qaydadadır):
 *   1. `images[]` doludursa → onu istifadə edirik (relevans sırası qorunur)
 *   2. `images[]` boşdursa → mövcud `imageUrl` bir namizəd kimi qaytarılır
 *   3. O da yoxdursa → boş siyahı; `DestinationImage` universal fallback göstərir
 *
 * @param {{images?: unknown, imageUrl?: unknown} | null | undefined} destination
 * @returns {Array<ReturnType<typeof normaliseImageCandidate>>}
 */
export function toImageCandidates(destination) {
  const raw = Array.isArray(destination?.images) ? destination.images : [];
  const seen = new Set();
  const candidates = [];

  for (const item of raw) {
    const candidate = normaliseImageCandidate(item);
    if (!candidate || seen.has(candidate.url)) continue;
    seen.add(candidate.url);
    candidates.push(candidate);
  }

  if (candidates.length > 0) return candidates;

  // images[] yoxdur → mövcud imageUrl bir namizəd olur
  const fallbackUrl = normaliseImageUrl(destination?.imageUrl);
  if (!isUsableImageUrl(fallbackUrl)) return [];

  return [
    {
      id: null,
      url: fallbackUrl,
      fullUrl: fallbackUrl,
      photographer: null,
      photographerUrl: null,
      unsplashUrl: null,
    },
  ];
}
