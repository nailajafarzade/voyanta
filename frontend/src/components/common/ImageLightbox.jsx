import PropTypes from "prop-types";
import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { FaTimes } from "react-icons/fa";
import { isUsableImageUrl } from "../../utils/imageCandidates";

/**
 * Tam ekran şəkil baxışı (lightbox) — frontend-only, backendə toxunmur.
 *
 * • `fullUrl` varsa istifadə olunur (daha yüksək keyfiyyət), yoxdursa `url`
 * • `object-contain` + `max-h/max-w` → ŞƏKİL HƏR ZAMAN HESABLANIR, HEÇ VAXT
 *   dartılmır (mənşə nisbətləri qorunur)
 * • `Escape` ilə bağlanır, fonun özünə kliklədikdə bağlanır
 * • Mobil + masaüstü (çərçivə boşluqları, `safe-area` icazəsi)
 * • Attribution YALNIZ backend-in göndərdiyi sahələrdən gəlir
 *   (`photographer`, `photographerUrl`, `unsplashUrl`) — heç bir ad/URL
 *   sabit kodlanmır, heç nə uydurulmur
 *
 * Render `document.body`-ə portal ilə atılır: kartların 3D `transform`/`perspective`
 * konteynerləri `position: fixed` elementləri əhatə edə bilərdi.
 */

/** Yalnız təhlükəsiz URL-ləri linkə çevirir. */
function safeLink(value) {
  return isUsableImageUrl(value) ? value : null;
}

function ImageLightbox({ image, title, onClose }) {
  const closeButtonRef = useRef(null);
  const previouslyFocusedRef = useRef(null);

  const fullUrl = safeLink(image?.fullUrl);
  const cardUrl = safeLink(image?.url);
  const source = fullUrl ?? cardUrl;

  const photographerUrl = safeLink(image?.photographerUrl);
  const unsplashUrl = safeLink(image?.unsplashUrl);
  const photographer =
    typeof image?.photographer === "string" && image.photographer.trim()
      ? image.photographer.trim()
      : null;

  const hasAttribution = Boolean(photographer || unsplashUrl);

  // Escape ilə bağlanma
  useEffect(() => {
    if (!source) return undefined;
    const onKeyDown = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [source, onClose]);

  // Arxa plan sürüşməsinin qarşısını al (mobil üçün vacib)
  useEffect(() => {
    if (!source) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [source]);

  // Fokusu bağlama düyməsinə köçür, bağlananda geri qaytarır
  useEffect(() => {
    if (!source) return undefined;
    previouslyFocusedRef.current = document.activeElement;
    closeButtonRef.current?.focus();
    return () => {
      if (
        previouslyFocusedRef.current instanceof HTMLElement &&
        document.contains(previouslyFocusedRef.current)
      ) {
        previouslyFocusedRef.current.focus();
      }
    };
  }, [source]);

  if (!source) return null;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      aria-label={title || "Şəkil"}
      className="fixed inset-0 z-[120] flex h-[100dvh] w-screen flex-col bg-ink-900/95 backdrop-blur-sm"
    >
      {/* fon qatı — boş yerə klikdikdə bağlanır */}
      <button
        type="button"
        aria-label="Şəkli bağla"
        tabIndex={-1}
        onClick={onClose}
        className="absolute inset-0 h-full w-full cursor-zoom-out"
      />

      {/* başlıq / bağlama düyməsi */}
      <div className="relative z-10 flex items-start justify-between gap-4 px-4 pt-[calc(0.75rem+env(safe-area-inset-top))] sm:px-6 sm:pt-5">
        <p className="min-w-0 pt-2 text-sm font-semibold text-white/85 drop-shadow-sm sm:text-base">
          {title}
        </p>

        <button
          ref={closeButtonRef}
          type="button"
          onClick={onClose}
          aria-label="Bağla (Esc)"
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-white/10 text-white/85 backdrop-blur-md transition-all duration-200 hover:bg-white/20 hover:text-white active:scale-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 sm:h-11 sm:w-11"
        >
          <FaTimes className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      {/* şəkil — əsl nisbətlər qorunur, dartılmır */}
      <div className="relative z-10 flex min-h-0 flex-1 items-center justify-center px-3 py-4 sm:px-6 sm:py-6">
        <img
          src={source}
          alt={title || ""}
          draggable={false}
          className="max-h-full max-w-full rounded-2xl object-contain shadow-lift"
        />
      </div>

      {/* attribution — yalnız backend-dən gələn məlumat */}
      {hasAttribution && (
        <div className="relative z-10 flex flex-wrap items-center justify-center gap-x-2 gap-y-1 px-4 pb-[calc(0.9rem+env(safe-area-inset-bottom))] pt-2 text-center text-[11px] text-white/60 sm:text-xs">
          {photographer && (
            <span>
              <span>Foto: </span>
              {photographerUrl ? (
                <a
                  href={photographerUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-semibold text-white/90 underline decoration-white/30 underline-offset-2 transition-colors hover:text-white hover:decoration-white"
                >
                  {photographer}
                </a>
              ) : (
                <span className="font-semibold text-white/90">
                  {photographer}
                </span>
              )}
            </span>
          )}

          {unsplashUrl && (
            <a
              href={unsplashUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="font-semibold text-white/90 underline decoration-white/30 underline-offset-2 transition-colors hover:text-white hover:decoration-white"
            >
              Unsplash
            </a>
          )}
        </div>
      )}
    </div>,
    document.body
  );
}

/** Backend `ImageCandidateResponse` forması (bax: utils/imageCandidates.js). */
export const imageCandidateShape = PropTypes.shape({
  id: PropTypes.string,
  url: PropTypes.string.isRequired,
  fullUrl: PropTypes.string,
  photographer: PropTypes.string,
  photographerUrl: PropTypes.string,
  unsplashUrl: PropTypes.string,
});

ImageLightbox.propTypes = {
  image: imageCandidateShape,
  title: PropTypes.string,
  onClose: PropTypes.func.isRequired,
};

export default ImageLightbox;
