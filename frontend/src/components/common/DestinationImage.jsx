
import PropTypes from "prop-types";
import { useEffect, useMemo, useState } from "react";
import { FaExpand } from "react-icons/fa";
import {
  isUsableImageUrl,
  normaliseImageUrl,
} from "../../utils/imageCandidates";
import { getSeason } from "../../utils/season";
import ImageLightbox from "./ImageLightbox";

/**
 * Səhifə üzrə şəkil TƏYİNİ backend-dən gələn `images[]` namizədləri ilə işləyir.
 *
 * Burda istiqamət adına görə şəkil seçimi YOXDUR — şəkli backend (Unsplash + keş)
 * qərar verir. AI planlarındakı əvvəlcədən təyin edilməmiş istiqamətlər
 * (Greenland, Svalbard, ...) üçün də eyni qaydada işləyir: yalnız `src` verilir.
 *
 * Yeni imkanlar (bütünü frontend-only):
 *   • `image`      → seçilmiş namizəd (bax: utils/imageAssignment.js).
 *                    Səhifə üzrə təkrar URL-lərin qarşısı burada alınır.
 *   • `candidates` → həmin istiqamətin bütün namizədləri. Seçilmiş şəkil
 *                    yüklənməsə növbəti RELEVANT namizəd sınanır.
 *   • `zoomable`   → şəklin üzərində tam ekran baxış düyməsi görünür.
 *
 * Fallback zənciri: `image.url` → `src` → qalan namizədlər → universal səyahət
 * gradient-i. Heç vaxt saxta URL yaratmır, heç vaxt qırıq şəkil ikonu göstərmir
 * və bir istiqamətin şəkli olmasa belə kart düzəni dağılmır.
 *
 * `season` verildikdə şəklin üzərinə mövsüm rəng qatı əlavə olunur ki, vizual
 * olaraq cari ilə uyğun görünsün (PO tələbi #1).
 */

/**
 * Sınanacaq URL-lərin sırasını qurur.
 * Əvvəlcə seçilmiş namizəd, sonra köhnə `src` (planın saxlanmış üz qabığı),
 * qalan namizədlər — beləcə `src` namizədlər tərəfindən üstələnmir.
 * @returns {string[]}
 */
function buildSources(image, candidates, src) {
  const list = [];
  const add = (value) => {
    const url = normaliseImageUrl(value);
    if (isUsableImageUrl(url) && !list.includes(url)) list.push(url);
  };

  add(image?.url);
  add(src);
  for (const candidate of candidates ?? []) {
    if (candidate !== image) add(candidate?.url);
  }

  return list;
}

function DestinationImage({
  src,
  image = null,
  candidates = null,
  alt = "",
  title = "",
  className = "",
  season = false,
  eager = false,
  zoomable = false,
}) {
  const [failedIndex, setFailedIndex] = useState(0);
  const [isLoaded, setIsLoaded] = useState(false);
  const [isZoomed, setIsZoomed] = useState(false);

  const sources = useMemo(
    () => buildSources(image, candidates, src),
    [image, candidates, src]
  );

  // Yeni şəkil gələndə (və ya səhifə dəyişəndə) vəziyyəti sıfırla
  useEffect(() => {
    setFailedIndex(0);
    setIsLoaded(false);
    setIsZoomed(false);
  }, [sources]);

  const resolved = sources[failedIndex] ?? null;
  const seasonClass = season ? getSeason().grade : "";
  const canZoom = zoomable && Boolean(resolved);

  // Şəkil yüklənmədiyi üçün universal fallback (kart heç vaxt qırılmır)
  if (!resolved) {
    return (
      <div
        role="img"
        aria-label={alt}
        className={`${className} ${seasonClass} bg-gradient-to-tr from-slate-300 via-slate-200 to-slate-400`}
      />
    );
  }

  // Konteyner <button>/<a> icinde ola biler ucun duyme <span role="button">
  // olaraq qurulur — bu, etibarsiz ici-ici <button> yaratmir ve valideynin
  // kecid klikini (stopPropagation) pozur.
  const zoomControl = canZoom ? (
    <span
      role="button"
      tabIndex={0}
      aria-label="Şəkli tam ekran aç"
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        setIsZoomed(true);
      }}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          event.stopPropagation();
          setIsZoomed(true);
        }
      }}
      className="absolute right-2 top-2 z-20 flex h-8 w-8 items-center justify-center rounded-full bg-ink-900/45 text-white/85 opacity-0 backdrop-blur-md transition-all duration-300 hover:bg-ink-900/70 hover:text-white focus-visible:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 group-hover:opacity-100 active:scale-90 sm:h-9 sm:w-9"
    >
      <FaExpand className="h-3.5 w-3.5" aria-hidden="true" />
    </span>
  ) : null;

  return (
    <>
      <div className={`${className} ${seasonClass} voy-grade relative bg-slate-200`}>
        <img
          src={resolved}
          alt={alt}
          loading={eager ? "eager" : "lazy"}
          decoding="async"
          onError={() => setFailedIndex((index) => index + 1)}
          onLoad={() => setIsLoaded(true)}
          className={`h-full w-full object-cover transition-opacity duration-700 ${
            isLoaded ? "opacity-100" : "opacity-0"
          }`}
        />

        {/* Şəkil yüklənməyənə qədər skeleton */}
        {!isLoaded && (
          <div
            aria-hidden="true"
            className="voy-skeleton pointer-events-none absolute inset-0"
          />
        )}

        {zoomControl}
      </div>

      {isZoomed && (
        <ImageLightbox
          image={image ?? { url: resolved, fullUrl: resolved }}
          title={title || alt}
          onClose={() => setIsZoomed(false)}
        />
      )}
    </>
  );
}

DestinationImage.propTypes = {
  src: PropTypes.string,
  image: PropTypes.shape({
    url: PropTypes.string,
    fullUrl: PropTypes.string,
    photographer: PropTypes.string,
    photographerUrl: PropTypes.string,
    unsplashUrl: PropTypes.string,
  }),
  candidates: PropTypes.arrayOf(
    PropTypes.shape({ url: PropTypes.string })
  ),
  alt: PropTypes.string,
  title: PropTypes.string,
  className: PropTypes.string,
  season: PropTypes.bool,
  eager: PropTypes.bool,
  zoomable: PropTypes.bool,
};

export default DestinationImage;

