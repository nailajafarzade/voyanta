import PropTypes from "prop-types";
import { useState } from "react";
import { LOVED_PLACES } from "../../../data/lovedPlaces";
import SectionHeading from "../../../components/common/SectionHeading";
import Reveal from "../../../components/common/Reveal";
import LovedPlaceCard from "../../../components/common/LovedPlaceCard";
import LovedPlaceModal from "../../../components/common/LovedPlaceModal";

/**
 * "Ən çox sevilən yerlər" — STATIC local data, no backend, no Unsplash.
 * 4 destinations (Bangkok, Paris, Dubai, London) x 4 local images each.
 * Hover cycles images; click opens detail modal.
 */
function PopularPlaces() {
    const [selected, setSelected] = useState(null);

    return (
        <section className="relative overflow-hidden bg-canvas py-20 lg:py-24">
            <div
                aria-hidden="true"
                className="pointer-events-none absolute -right-24 top-1/4 h-72 w-72 rounded-full bg-sun-100/40 blur-3xl"
            />
            <div
                aria-hidden="true"
                className="pointer-events-none absolute -left-32 top-3/4 h-80 w-80 rounded-full bg-brand-100/30 blur-3xl"
            />

            <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <Reveal>
                    <SectionHeading
                        eyebrow={{ text: "Populyar", tone: "sun", icon: "⭐" }}
                        title="Ən çox sevilən yerlər"
                        description="Səyahətçilərin ən çox seçdiyi 4 klassik istiqamət."
                    />
                </Reveal>

                <div className="mt-10 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
                    {LOVED_PLACES.map((place, index) => (
                        <LovedPlaceCard
                            key={place.id}
                            place={place}
                            index={index}
                            onOpen={setSelected}
                        />
                    ))}
                </div>
            </div>

            <LovedPlaceModal place={selected} onClose={() => setSelected(null)} />
        </section>
    );
}

/** `useDestinations` shape — kept for PersonalizedPlaces import. */
export const sectionStateShape = PropTypes.shape({
    places: PropTypes.array.isRequired,
    allPlaces: PropTypes.array.isRequired,
    isLoading: PropTypes.bool.isRequired,
    error: PropTypes.string,
    authRequired: PropTypes.bool.isRequired,
    reload: PropTypes.func.isRequired,
});

/** `createImagePlan` Map — kept for PersonalizedPlaces import. */
export const imagePlanShape = PropTypes.instanceOf(Map);

PopularPlaces.propTypes = {};

export default PopularPlaces;
