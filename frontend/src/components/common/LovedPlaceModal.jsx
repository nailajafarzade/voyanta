import PropTypes from "prop-types";
import { useCallback, useEffect, useState } from "react";

/**
 * Loved-place detail modal — gallery + info, local images only.
 * Closable via button, backdrop click, Escape. Locks body scroll.
 */
function LovedPlaceModal({ place, onClose }) {
  const [activeImage, setActiveImage] = useState(0);

  useEffect(() => {
    setActiveImage(0);
  }, [place?.id]);

  const close = useCallback(() => onClose?.(), [onClose]);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === "Escape") close();
      if (!place) return;
      if (e.key === "ArrowRight") setActiveImage((i) => (i + 1) % place.images.length);
      if (e.key === "ArrowLeft") setActiveImage((i) => (i - 1 + place.images.length) % place.images.length);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [close, place]);

  useEffect(() => {
    if (!place) return undefined;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [place]);

  if (!place) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-ink-900/60 p-0 backdrop-blur-sm sm:items-center sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-label={`${place.name} ətraflı məlumat`}
      onClick={close}
    >
      <div
        className="relative max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-t-3xl bg-white shadow-lift sm:rounded-3xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="relative aspect-[16/10] w-full overflow-hidden bg-slate-200 sm:rounded-t-3xl">
          {place.images.map((src, i) => (
            <img
              key={`${place.id}-${i}`}
              src={src}
              alt={i === 0 ? `${place.name}, ${place.country}` : `${place.name} foto ${i + 1}`}
              loading={i === 0 ? "eager" : "lazy"}
              decoding="async"
              draggable="false"
              aria-hidden={i === activeImage ? "false" : "true"}
              className={`absolute inset-0 h-full w-full object-cover transition-opacity duration-500 ${
                i === activeImage ? "opacity-100" : "pointer-events-none opacity-0"
              }`}
            />
          ))}
          <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/5 to-transparent" />
          <button
            type="button"
            onClick={close}
            aria-label="Bağla"
            autoFocus
            className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-ink-900/55 text-lg font-bold text-white backdrop-blur-md transition-all hover:bg-ink-900/75 active:scale-90"
          >
            ✕
          </button>
          <div className="absolute inset-x-0 bottom-0 flex items-end justify-between gap-3 p-5">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.15em] text-white/75">
                {place.country} {place.flag}
              </p>
              <h3 className="mt-1 text-2xl font-extrabold text-white drop-shadow-sm">{place.name}</h3>
            </div>
            <span className="shrink-0 rounded-full bg-white/90 px-3 py-1.5 text-[11px] font-bold text-ink-900 backdrop-blur-sm">
              {place.annualVisitors}
            </span>
          </div>
        </div>
        {place.images.length > 1 && (
          <div className="flex items-center justify-center gap-2 px-5 pt-4" aria-label="Şəkillər">
            {place.images.map((src, i) => (
              <button
                key={`${place.id}-thumb-${i}`}
                type="button"
                aria-label={`${place.name} şəkil ${i + 1}`}
                aria-pressed={i === activeImage}
                onClick={() => setActiveImage(i)}
                className={`h-12 w-16 overflow-hidden rounded-xl ring-2 transition-all ${
                  i === activeImage ? "ring-brand-500" : "opacity-60 ring-transparent hover:opacity-100"
                }`}
              >
                <img src={src} alt="" aria-hidden="true" loading="lazy" decoding="async" className="h-full w-full object-cover" />
              </button>
            ))}
          </div>
        )}

        <div className="space-y-5 p-5 sm:p-6">
          <section>
            <h4 className="text-sm font-extrabold uppercase tracking-wider text-ink-900">
              Niyə insanlar buranı seçir?
            </h4>
            <p className="mt-2 text-sm leading-relaxed text-ink-600">{place.whyPeopleChoose}</p>
          </section>

          <section>
            <h4 className="text-sm font-extrabold uppercase tracking-wider text-ink-900">Haqqında</h4>
            <p className="mt-2 text-sm leading-relaxed text-ink-600">{place.about}</p>
          </section>

          <section>
            <h4 className="text-sm font-extrabold uppercase tracking-wider text-ink-900">Məşhur yerlər</h4>
            <ul className="mt-2.5 grid grid-cols-1 gap-2 sm:grid-cols-2">
              {place.popularPlaces.map((item) => (
                <li
                  key={item}
                  className="flex items-center gap-2 rounded-2xl bg-slate-50 px-3.5 py-2.5 text-sm font-semibold text-ink-700"
                >
                  <span aria-hidden="true" className="text-brand-500">📍</span>
                  {item}
                </li>
              ))}
            </ul>
          </section>
        </div>
      </div>
    </div>
  );
}

LovedPlaceModal.propTypes = {
  place: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    country: PropTypes.string.isRequired,
    flag: PropTypes.string,
    annualVisitors: PropTypes.string.isRequired,
    about: PropTypes.string.isRequired,
    whyPeopleChoose: PropTypes.string.isRequired,
    popularPlaces: PropTypes.arrayOf(PropTypes.string).isRequired,
    images: PropTypes.arrayOf(PropTypes.string).isRequired,
  }),
  onClose: PropTypes.func.isRequired,
};

export default LovedPlaceModal;
