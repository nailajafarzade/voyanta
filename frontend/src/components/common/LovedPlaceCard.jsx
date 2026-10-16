import PropTypes from "prop-types";
import { useEffect, useRef, useState } from "react";
import Reveal from "./Reveal";
import FavoriteButton from "./FavoriteButton";
import { useBackendPlaces } from "../../hooks/useBackendPlaces";

const HOVER_MS = 1400;

/**
 * Static loved-place card with hover/touch image cycling.
 * Cycles ONLY its own 4 images (crossfade, fixed aspect ratio, no layout shift).
 * Desktop: hover starts cycling. Touch: tap cycles one image; card button
 * opens the detail modal (parent onOpen receives the place).
 */
function LovedPlaceCard({ place, index, onOpen }) {
  const [active, setActive] = useState(0);
  const [isHovering, setIsHovering] = useState(false);
  const timer = useRef(null);

  // Static slug (e.g. "paris") -> real backend place (UUID) for the wishlist API.
  // Unmatched (no backend row) -> visible-but-disabled heart, never a fake ID.
  const { resolveBackendPlace } = useBackendPlaces();
  const backendPlace = resolveBackendPlace(place?.name);

  useEffect(() => {
    if (!isHovering || place.images.length < 2) return undefined;
    timer.current = window.setInterval(() => {
      setActive((i) => (i + 1) % place.images.length);
    }, HOVER_MS);
    return () => window.clearInterval(timer.current);
  }, [isHovering, place.images.length]);

  useEffect(() => () => window.clearInterval(timer.current), []);

  const cycleOnce = () => {
    if (place.images.length < 2) return;
    setActive((i) => (i + 1) % place.images.length);
  };

  return (
    <Reveal delay={index * 80}>
      <div
        className="group relative flex h-full flex-col overflow-hidden rounded-3xl bg-white text-left shadow-soft ring-1 ring-slate-100 transition-all duration-500 hover:-translate-y-1 hover:shadow-lift"
        onMouseEnter={() => setIsHovering(true)}
        onMouseLeave={() => {
          setIsHovering(false);
          setActive(0);
        }}
      >
        <button
          type="button"
          onClick={() => onOpen(place)}
          aria-label={`${place.name} ətraflı bax`}
          className="flex h-full flex-col text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        >
          <span className="relative block aspect-[4/3] w-full overflow-hidden bg-slate-200">
            {place.images.map((src, i) => (
              <img
                key={`${place.id}-${i}`}
                src={src}
                alt={i === 0 ? `${place.name}, ${place.country}` : `${place.name} foto ${i + 1}`}
                loading={i === 0 ? "eager" : "lazy"}
                decoding="async"
                draggable="false"
                aria-hidden={i === active ? "false" : "true"}
                className={`absolute inset-0 h-full w-full object-cover transition-all duration-700 ease-out group-hover:scale-105 ${
                  i === active ? "opacity-100" : "pointer-events-none opacity-0"
                }`}
              />
            ))}
            <span className="absolute inset-0 bg-gradient-to-t from-black/45 via-transparent to-transparent" />
            <span className="absolute left-4 top-4 inline-flex items-center gap-1 rounded-full bg-ink-900/55 px-2.5 py-1 text-[10px] font-bold text-white backdrop-blur-md">
              <span aria-hidden="true">📸</span>
              {active + 1}/{place.images.length}
            </span>
          </span>

          <span className="flex flex-1 flex-col p-5">
            <span className="text-[11px] font-bold uppercase tracking-[0.15em] text-ink-400">
              {place.country} {place.flag}
            </span>
            <span className="mt-1 text-xl font-extrabold tracking-tight text-ink-900">
              {place.name}
            </span>
            <span className="mt-1.5 text-xs font-semibold text-ink-500">
              👥 {place.annualVisitors}
            </span>
            <span className="mt-3 inline-flex items-center gap-1.5 text-xs font-bold text-brand-600">
              Ətraflı bax
              <span aria-hidden="true" className="transition-transform duration-300 group-hover:translate-x-1">→</span>
            </span>
          </span>
        </button>

        {/* Favorite heart — sibling of the modal-open button (never nested),
            overlay on the image with z-30 so gradients never cover it.
            Outer span stops the click so the modal never opens from the heart. */}
        <div
          className="absolute right-4 top-4 z-30"
          onClick={(e) => e.stopPropagation()}
          onKeyDown={(e) => e.stopPropagation()}
        >
          <FavoriteButton
            place={
              backendPlace ?? {
                title: place.name,
                subtitle: place.country,
              }
            }
            size="sm"
          />
        </div>

        {place.images.length > 1 && (
          <span className="absolute bottom-4 right-4 flex gap-1.5">
            {place.images.map((_, i) => (
              <button
                key={`${place.id}-dot-${i}`}
                type="button"
                tabIndex={-1}
                aria-hidden="true"
                onClick={(e) => {
                  e.stopPropagation();
                  setActive(i);
                }}
                onTouchStart={(e) => {
                  e.stopPropagation();
                }}
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  i === active ? "w-5 bg-white" : "w-1.5 bg-white/50 hover:bg-white/80"
                }`}
              />
            ))}
          </span>
        )}

        <button
          type="button"
          aria-label="Növbəti şəkil"
          className="absolute left-3 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/0 text-white opacity-0 transition-all duration-300 hover:bg-white/90 hover:text-ink-900 focus-visible:bg-white/90 focus-visible:text-ink-900 focus-visible:opacity-100 group-hover:opacity-100 sm:hidden"
          onClick={(e) => {
            e.stopPropagation();
            cycleOnce();
          }}
        >
          →
        </button>
      </div>
    </Reveal>
  );
}

LovedPlaceCard.propTypes = {
  place: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    country: PropTypes.string.isRequired,
    flag: PropTypes.string,
    annualVisitors: PropTypes.string.isRequired,
    images: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  index: PropTypes.number,
  onOpen: PropTypes.func.isRequired,
};

export default LovedPlaceCard;
