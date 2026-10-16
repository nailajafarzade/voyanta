import PropTypes from "prop-types";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { getPlanImage } from "../../utils/imageAssignment";
import { getDestinationDetails } from "../../utils/destinationDetails";
import { seasonStatus } from "../../utils/season";
import DestinationImage from "../common/DestinationImage";
import Reveal from "../common/Reveal";
import FavoriteButton from "../common/FavoriteButton";

function DestinationCard({
                             place,
                             imagePlan,
                             sectionKey,
                             index,
                             onOpenDetails,
                             badge,
                         }) {
    const navigate = useNavigate();
    const [imageError] = useState(false);
    const details = getDestinationDetails(place);
    const status = seasonStatus(details.bestMonths);
    const image = getPlanImage(imagePlan, sectionKey, place);
    const src = image?.url || place.imageUrl;
    const candidates = place.images || [];

    const handleClick = () => {
        if (onOpenDetails) {
            onOpenDetails(place.id);
        } else {
            navigate(`/destination/${place.id}`);
        }
    };

    return (
        <Reveal delay={index * 80}>
            <button
                type="button"
                onClick={handleClick}
                className="group relative block w-full overflow-hidden rounded-3xl bg-ink-900 text-left shadow-[0_10px_30px_-12px_rgba(15,23,42,0.35)] transition-all duration-500 hover:-translate-y-1 hover:shadow-lift focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
                <div className="relative aspect-[3/4] w-full">
                    {!imageError ? (
                        <DestinationImage
                            image={image}
                            candidates={candidates}
                            src={src}
                            alt={place.title}
                            className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-110"
                            loading="lazy"
                            width={600}
                            height={800}
                        />
                    ) : (
                        <div className="absolute inset-0 bg-gradient-to-br from-brand-200 to-mint-200" />
                    )}

                    <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />

                    {/* Top-left badge */}
                    {(badge || status.state === "peak") && (
                        <div className="absolute left-4 top-4">
                            <span className="inline-flex items-center gap-1.5 rounded-full border border-white/30 bg-white/20 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-white backdrop-blur-md">
                                {badge || (
                                    <>
                                        <span aria-hidden="true">✨</span>
                                        Ən yaxşı vaxt
                                    </>
                                )}
                            </span>
                        </div>
                    )}

                    {/* Top-right favorite button */}
                    <div className="absolute right-4 top-4 z-30">
                        <FavoriteButton place={place} size="sm" />
                    </div>

                    {/* Bottom content */}
                    <div className="absolute inset-x-0 bottom-0 p-5">
                        <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-white/70">
                            {place.subtitle}
                        </p>
                        <h3
                            className="mt-1.5 font-extrabold leading-tight text-white sm:text-xl"
                            style={{
                                fontFamily:
                                    "'Fraunces', 'Playfair Display', Georgia, serif",
                                fontSize: "clamp(18px, 2.5vw, 24px)",
                            }}
                        >
                            {place.title}
                        </h3>
                        <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-white/75">
                            {details.headline}
                        </p>

                        {/* Hover: Explore line */}
                        <span
                            className="mt-3 inline-flex items-center gap-1.5 text-xs font-semibold text-white opacity-0 transition-all duration-300 group-hover:translate-x-1 group-hover:opacity-100"
                            aria-hidden="true"
                        >
                            Kəşf et
                            <span aria-hidden="true">→</span>
                        </span>
                    </div>
                </div>
            </button>
        </Reveal>
    );
}

DestinationCard.propTypes = {
    place: PropTypes.shape({
        id: PropTypes.string.isRequired,
        title: PropTypes.string.isRequired,
        subtitle: PropTypes.string.isRequired,
        imageUrl: PropTypes.string,
        images: PropTypes.array,
    }).isRequired,
    imagePlan: PropTypes.instanceOf(Map),
    sectionKey: PropTypes.string.isRequired,
    index: PropTypes.number,
    onOpenDetails: PropTypes.func,
    badge: PropTypes.node,
};

export default DestinationCard;