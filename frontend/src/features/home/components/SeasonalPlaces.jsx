import { useMemo, useState } from "react";
import { SEASONS } from "../../../data/seasons";
import { getSeason } from "../../../utils/season";
import SectionHeading from "../../../components/common/SectionHeading";
import SeasonDestinationCard from "../../../components/common/SeasonDestinationCard";

/**
 * Static Seasons section — 4 seasons x 4 destinations, local images only.
 * No backend, no Unsplash. Data-driven from `src/data/seasons.js`.
 */
function SeasonalPlaces() {
    const currentSeason = useMemo(() => getSeason(), []);
    const defaultKey = SEASONS.some((s) => s.key === currentSeason.key)
        ? currentSeason.key
        : "spring";
    const [activeSeason, setActiveSeason] = useState(defaultKey);

    const active = SEASONS.find((s) => s.key === activeSeason) ?? SEASONS[0];

    return (
        <section className="relative overflow-hidden bg-canvas py-20 lg:py-24">
            <div
                aria-hidden="true"
                className="pointer-events-none absolute -right-24 bottom-0 h-72 w-72 rounded-full bg-mint-100/50 blur-3xl"
            />

            <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
                <SectionHeading
                    eyebrow={{
                        text: `Mövsüm — ${active.azLabel ?? active.label}`,
                        tone: "mint",
                        icon: active.icon,
                    }}
                    title="Mövsümə uyğun kəşf et"
                    description="Dörd mövsüm, hər mövsüm üçün dörd seçilmiş istiqamət. Hamısı yerli şəkillərlə hazırlanıb."
                />

                <div className="mt-8 flex flex-wrap gap-2" role="tablist" aria-label="Mövsüm tabları">
                    {SEASONS.map((s) => {
                        const isActive = activeSeason === s.key;
                        return (
                            <button
                                key={s.key}
                                type="button"
                                role="tab"
                                aria-selected={isActive}
                                onClick={() => setActiveSeason(s.key)}
                                className={`rounded-full px-5 py-2.5 text-sm font-semibold transition-all duration-300 ${
                                    isActive
                                        ? "bg-ink-900 text-white shadow-soft"
                                        : "bg-white text-ink-500 hover:bg-slate-100"
                                }`}
                            >
                                <span aria-hidden="true" className="mr-1.5">{s.icon}</span>
                                {s.azLabel ?? s.label}
                            </button>
                        );
                    })}
                </div>

                <p className="mt-4 max-w-xl text-sm leading-relaxed text-ink-500">
                    {active.blurb}
                </p>

                <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
                    {active.destinations.map((destination, index) => (
                        <SeasonDestinationCard
                            key={destination.id}
                            destination={destination}
                            index={index}
                        />
                    ))}
                </div>
            </div>
        </section>
    );
}

export default SeasonalPlaces;