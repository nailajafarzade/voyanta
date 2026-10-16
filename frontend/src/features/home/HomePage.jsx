import { useMemo } from "react";
import HeroSection from "./components/HeroSection";
import HowItWorks from "./components/HowItWorks";
import PopularPlaces from "./components/PopularPlaces";
import PersonalizedPlaces from "./components/PersonalizedPlaces";
import SeasonalPlaces from "./components/SeasonalPlaces";
import { useAuth } from "../../context/authContext";
import { useDestinations, useImagePlan } from "../../hooks/useDestinations";

/**
 * Ana səhifə — tövsiyə bölmələri və ŞƏKİL PLANININ sahibi.
 * Sənə özəl marşrut 100% lokal statik datadır (backend sorğusu yoxdur).
 */
function HomePage() {
    const { isAuthenticated, isLoading: authLoading } = useAuth();

    const hero = useDestinations({
        isAuthenticated,
        isAuthLoading: authLoading,
        source: "featured",
        limit: 4,
    });

    const imagePlan = useImagePlan(
        useMemo(
            () => [
                { key: "hero", places: hero.allPlaces },
            ],
            [hero.allPlaces]
        )
    );

    return (
        <main className="pb-20 md:pb-28">
            <HeroSection state={hero} imagePlan={imagePlan} />
            <HowItWorks />
            <PopularPlaces />
            <PersonalizedPlaces />
            <SeasonalPlaces />
        </main>
    );
}

export default HomePage;
