import PropTypes from "prop-types";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import DestinationImage from "./DestinationImage";
import FavoriteButton from "./FavoriteButton";
import { getDestinationDetails } from "../../utils/destinationDetails";
import { monthRange, seasonStatus } from "../../utils/season";

/**
 * Səyahət poçt kartı — 3D flip effekti (PO tələbi #5).
 *
 *   Ön üz: şəkil + ad + etiket
 *   Arxa üz: qısa məlumat + əsas fəaliyyətlər + "Ətraflı" / "Planla"
 *
 * Desktop-da hover (və fokus) ilə çevrilir; mobil/touch-da klikləyib
 * "çevir" düyməsi ilə idarə olunur. CSS `.voy-flip` sinfindəki
 * `data-flipped` atributu ilə işləyir.
 */
function PostcardCard({
  place,
  rank,
  image,
  zoomable = false,
  onOpenDetails,
  onPlan,
  badge,
}) {
  const navigate = useNavigate();
  const [isFlipped, setIsFlipped] = useState(false);

  const details = getDestinationDetails(place);
  const status = seasonStatus(details.bestMonths);

  const openDetails = () => {
    if (onOpenDetails) onOpenDetails(place.id);
    else navigate(`/destination/${place.id}`);
  };

  return (
    <div
      className="voy-flip h-full min-h-[320px] w-full"
      data-flipped={isFlipped ? "true" : "false"}
    >
      <div className="voy-flip-inner group rounded-[28px] shadow-soft ring-1 ring-slate-200/70 transition-shadow duration-500 hover:shadow-lift">
        {/* ================= ÖN ÜZ ================= */}
        <div className="voy-face voy-face-front rounded-[28px] bg-ink-900">
          <DestinationImage
            image={image}
            candidates={place.images}
            src={place.imageUrl}
            alt={place.title}
            className="absolute inset-0 h-full w-full"
            eager={rank === 0}
            zoomable={zoomable}
          />

          {/* qrafik qat */}
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-ink-900 via-ink-900/25 to-transparent opacity-95" />
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-brand-500/0 via-transparent to-sun-500/10 opacity-0 transition-opacity duration-700 group-hover:opacity-100" />

          {rank !== undefined && (
            <div className="absolute left-4 top-4 flex items-center gap-1.5 rounded-full bg-ink-900/55 px-3 py-1.5 text-[11px] font-bold text-white backdrop-blur-md">
              <span aria-hidden="true">🔥</span>
              <span>Trend #{rank + 1}</span>
            </div>
          )}

          {badge}

          {/* Favorite heart — overlay on the image, above gradients (z-30).
              stopPropagation lives inside FavoriteButton, so the card never flips/navigates. */}
          <div className="absolute right-4 top-4 z-30">
            <FavoriteButton place={place} size="sm" />
          </div>

          <div className="absolute inset-x-0 bottom-0 p-5">
            <span className="mb-2 inline-block rounded-full bg-white/15 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-white/90 backdrop-blur-sm">
              {place.subtitle}
            </span>

            <h3 className="text-lg font-bold leading-tight text-white drop-shadow-sm">
              {place.title}
            </h3>

            <div className="mt-3 flex items-center gap-2 text-[11px] font-medium text-white/80">
              <span className="inline-flex h-1.5 w-1.5 rounded-full bg-white/80" />
              <span>{details.bestSeason ? "Mövsümə uyğun" : "Kəşf et"}</span>
              <span className="ml-auto transition-transform duration-500 group-hover:translate-x-1">
                →
              </span>
            </div>
          </div>
        </div>

        {/* ================= ARXA ÜZ ================= */}
        <div className="voy-face voy-face-back rounded-[28px] bg-gradient-to-br from-ink-900 via-ink-800 to-brand-900 p-5">
          <div className="flex h-full flex-col">
            <div className="flex items-start justify-between gap-2">
              <h3 className="text-base font-bold leading-tight text-white">
                {place.title}
              </h3>

              <button
                type="button"
                onClick={() => setIsFlipped((prev) => !prev)}
                aria-label={isFlipped ? "Kartı geri çevir" : "Kartı çevir"}
                className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/10 text-white/70 transition-all duration-200 hover:bg-white/20 hover:text-white active:scale-90"
              >
                <svg
                  className="h-3.5 w-3.5"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
                  <path d="M3 3v5h5" />
                </svg>
              </button>
            </div>

            <p className="mt-2.5 text-[13px] leading-relaxed text-white/70">
              {details.headline}
            </p>

            <ul className="mt-3.5 flex flex-wrap gap-1.5">
              {details.activities.slice(0, 4).map((activity) => (
                <li
                  key={activity.label}
                  className="rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-medium text-white/85"
                >
                  <span aria-hidden="true" className="mr-1">
                    {activity.icon}
                  </span>
                  {activity.label}
                </li>
              ))}
            </ul>

            <dl className="mt-3.5 space-y-1.5 border-t border-white/10 pt-3 text-[11px]">
              <div className="flex items-center justify-between gap-2">
                <dt className="text-white/45">Ən yaxşı dövr</dt>
                <dd className="font-semibold text-white/90">
                  {monthRange(details.bestMonths) ?? status.label}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-2">
                <dt className="text-white/45">Müddət</dt>
                <dd className="font-semibold text-white/90">
                  {details.stayLength}
                </dd>
              </div>
            </dl>

            <div className="mt-auto flex items-center gap-2 pt-4">
              <button
                type="button"
                onClick={openDetails}
                className="flex-1 rounded-full bg-white/95 px-4 py-2.5 text-[12px] font-bold text-ink-900 transition-all duration-200 hover:bg-white active:scale-95"
              >
                Ətraflı
              </button>

              {onPlan && (
                <button
                  type="button"
                  onClick={onPlan}
                  aria-label="Bu istiqamət üçün plan hazırla"
                  className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-500/90 text-white transition-all duration-200 hover:bg-brand-500 active:scale-95"
                >
                  <svg
                    className="h-4 w-4"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M5 12h14M13 6l6 6-6 6" />
                  </svg>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

PostcardCard.propTypes = {
  place: PropTypes.shape({
    id: PropTypes.string,
    title: PropTypes.string,
    subtitle: PropTypes.string,
    imageUrl: PropTypes.string,
    images: PropTypes.arrayOf(PropTypes.shape({ url: PropTypes.string })),
  }).isRequired,
  rank: PropTypes.number,
  image: PropTypes.shape({ url: PropTypes.string, fullUrl: PropTypes.string }),
  zoomable: PropTypes.bool,
  onOpenDetails: PropTypes.func,
  onPlan: PropTypes.func,
  badge: PropTypes.node,
};

export default PostcardCard;

