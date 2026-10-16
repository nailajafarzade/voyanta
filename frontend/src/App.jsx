import { Navigate, Route, Routes } from "react-router-dom";
import "./App.css";
import HomePage from "./features/home/HomePage";
import MainLayout from "./components/layout/MainLayout";
import ProfilePage from "./features/profile/ProfilePage";
import TravelItineraryPage from "./features/planning/TravelItineraryPage";
import LocationsDiscovered from "./features/planning/LocationsDiscovered";
import YourFavorites from "./features/wishlist/YourFavorites";
import PlanLoading from "./features/planning/PlanLoading";
import DestinationDetailPage from "./features/destination/DestinationDetailPage";
import ProtectedRoute from "./components/common/ProtectedRoute";
import WishlistSync from "./components/common/WishlistSync";

function App() {
    return (
        <>
            {/* Daxil olanda wishlist-i backend-dən yükləyir, çıxanda təmizləyir */}
            <WishlistSync />

            <Routes>
                <Route element={<MainLayout />}>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/travel/:planId" element={<TravelItineraryPage />} />
                    <Route path="/location" element={<LocationsDiscovered />} />
                    <Route path="/destination/:destinationId" element={<DestinationDetailPage />} />

                    {/* Yalnız daxil olmuş istifadəçi üçün */}
                    <Route element={<ProtectedRoute />}>
                        <Route path="/profil" element={<ProfilePage />} />
                        <Route path="/favorites" element={<YourFavorites />} />
                    </Route>
                </Route>

                {/* Plan hazırlanarkən gözləmə ekranı (Header/Footer olmadan) */}
                <Route path="/loading/:planId" element={<PlanLoading />} />

                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </>
    );
}

export default App;