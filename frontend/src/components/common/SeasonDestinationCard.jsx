import PropTypes from "prop-types";
import { useState } from "react";
import Reveal from "./Reveal";
import FavoriteButton from "./FavoriteButton";
import { useBackendPlaces } from "../../hooks/useBackendPlaces";

/**
 * Static season destination card — local image, expandable details.
 * Shows name, country, image, description, why-season, best months,
 * highlights. Click toggles the detail panel (consistent Voyanta card).
 */
function SeasonDestinationCard({ destination, index }) {
  const [isOpen, setIsOpen] = useState(false);

  // Static slug (e.g. "summer-bali") -> real backend place (UUID) for the wishlist API.
  const { resolveBackendPlace } = useBackendPlaces();
  const backendPlace = resolveBackendPlace(destination?.name);

  return (
    <Reveal delay={index * 80}>
      <article className="group flex h-full flex-col overflow-hidden rounded-3xl bg-white shadow-soft ring-1 ring-slate-100 transition-all duration-500 hover:-translate-y-1 hover:shadow-lift">
        <div className="relative aspect-[4/3] w-full overflow-hidden bg-slate-200">
          <img
            src={destination.image}
            alt={`${destination.name}, ${destination.country}`}
            loading="lazy"
            decoding="async"
            draggable="false"
            className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-105"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/10 to-transparent" />
          <span className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-white/90 px-3 py-1 text-[11px] font-bold text-ink-900 backdrop-blur-sm">
            <span aria-hidden="true">📅</span>
            {destination.bestMonths}
          </span>
          {/* Favorite heart — overlay on the image with z-30 so gradients never cover it. */}
          <div className="absolute right-4 top-4 z-30">
            <FavoriteButton
              place={
                backendPlace ?? {
                  title: destination.name,
                  subtitle: destination.country,
                }
              }
              size="sm"
            />
          </div>
          <div className="absolute inset-x-0 bottom-0 p-4">
            <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-white/75">
              {destination.country} {destination.flag}
            </p>
            <h3 className="mt-1 text-lg font-extrabold leading-tight text-white drop-shadow-sm">
              {destination.name}
            </h3>
          </div>
        </div>

        <div className="flex flex-1 flex-col p-5">
          <p className="text-sm leading-relaxed text-ink-600">{destination.description}</p>

          <ul className="mt-3 flex flex-wrap gap-1.5">
            {destination.highlights.map((tag) => (
              <li
                key={tag}
                className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold text-ink-600"
              >
                {tag}
              </li>
            ))}
          </ul>

          {isOpen && (
            <div className="mt-4 rounded-2xl bg-slate-50 p-4 text-xs leading-relaxed text-ink-600">
              <p>
                <span className="font-bold text-ink-900">Bu mövsüm niyə: </span>
                {destination.whySeason}
              </p>
              <p className="mt-2">
                <span className="font-bold text-ink-900">Ən yaxşı aylar: </span>
                {destination.bestMonths}
              </p>
            </div>
          )}

          <button
            type="button"
            onClick={() => setIsOpen((open) => !open)}
            aria-expanded={isOpen}
            className="mt-4 inline-flex w-fit items-center gap-1.5 text-xs font-bold text-brand-600 transition-colors hover:text-brand-700"
          >
            {isOpen ? "Bağla" : "Bu mövsüm niyə?"}
            <span aria-hidden="true" className={`transition-transform duration-300 ${isOpen ? "rotate-180" : ""}`}>
              ↓
            </span>
          </button>
        </div>
      </article>
    </Reveal>
  );
}

SeasonDestinationCard.propTypes = {
  destination: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    country: PropTypes.string.isRequired,
    flag: PropTypes.string,
    image: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    whySeason: PropTypes.string.isRequired,
    bestMonths: PropTypes.string.isRequired,
    highlights: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  index: PropTypes.number,
};

export default SeasonDestinationCard;
