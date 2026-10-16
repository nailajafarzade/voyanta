import { useDispatch, useSelector } from "react-redux";
import { FaHeart, FaRegHeart } from "react-icons/fa";

import { toggleFavorite } from "../../redux/wishlistThunks";
import { getDestinationDetails } from "../../utils/destinationDetails";
import { useDestinations } from "../../hooks/useDestinations";
import DestinationImage from "../../components/common/DestinationImage";
import { useAuth } from "../../context/authContext";
import { useAuthModal } from "../../context/authModalContext";
import { usePlanner } from "../../context/plannerContext";
import { CardSkeleton, EmptyState, ErrorState } from "../../components/common/States";

/**
 * "/location" — "Sənin üçün seçilmiş yerlər" (istiqamət kataloqu).
 *
 * Bu səhifə də backend-in `GET /api/recommendations/personalized` məlumatını
 * göstərir, ona görə gonaq üçün qorunur: `requireAuth: true` — gonaq heç bir
 * sorğu göndərmir və qeydiyyat promptunu görür (401/403 halında eyni vəziyyət).
 *
 * Kartlar heç bir səhifəyə keçmir (navigasiya yoxdur); şəkli isə backend-in
 * `imageUrl`-indən gəlir.
 */
function LocationsDiscovered() {
  const dispatch = useDispatch();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { openLogin } = useAuthModal();
  const { openPlanner } = usePlanner();

  const favorites = useSelector((state) => state.wishlist.favorites);

  const { places, isLoading, error, authRequired, reload } = useDestinations({
    isAuthenticated,
    isAuthLoading: authLoading,
    requireAuth: true,
  });

  const handleToggleFavorite = (e, place) => {
    e.stopPropagation();

    if (!isAuthenticated) {
      openLogin();
      return;
    }

    dispatch(toggleFavorite(place));
  };

  return (
    <section className="min-h-screen bg-canvas px-4 py-16 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-7xl">
        <div className="mb-10 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-ink-900 sm:text-4xl">
              Sənin üçün seçilmiş yerlər
            </h1>

            <p className="mt-2 text-sm text-ink-400">
              Səyahət etmək istədiyin yerləri favoritlərinə əlavə et.
            </p>
          </div>

          <button
            type="button"
            onClick={isAuthenticated ? openPlanner : openLogin}
            className="group inline-flex w-fit shrink-0 items-center gap-2 rounded-full bg-ink-900 px-6 py-3 text-sm font-semibold text-white shadow-soft transition-all duration-300 hover:bg-ink-800 active:scale-95"
          >
            <span>Plan qur</span>
            <span
              className="transition-transform duration-300 group-hover:translate-x-1"
              aria-hidden="true"
            >
              →
            </span>
          </button>
        </div>

        {isLoading && <CardSkeleton count={8} />}

        {/* gonaq / sessiya bitib → qeydiyyat promptu */}
        {authRequired && (
          <div className="overflow-hidden rounded-4xl border border-brand-100 bg-gradient-to-br from-white via-brand-50/60 to-mint-50/70 px-6 py-14 text-center shadow-soft sm:px-12">
            <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-white text-3xl shadow-soft ring-1 ring-slate-100">
              <span aria-hidden="true">✨</span>
            </div>

            <h2 className="text-xl font-extrabold tracking-tight text-ink-900 sm:text-2xl">
              Sənin üçün seçilmiş yerlər
            </h2>

            <p className="mx-auto mt-3 max-w-md text-sm leading-relaxed text-ink-500">
              Bu siyahı yalnız hesabına daxil olmuş istifadəçilər üçündür.
              Pulsuz qeydiyyatdan keç, sənə uyğun yerləri gör və favoritlərinə əlavə et.
            </p>

            <button
              type="button"
              onClick={openLogin}
              className="group mt-7 inline-flex items-center gap-2 rounded-full bg-ink-900 px-7 py-3.5 text-sm font-semibold text-white shadow-soft transition-all duration-300 hover:bg-ink-800 active:scale-95"
            >
              <span>Qeydiyyatdan keç</span>
              <span
                className="transition-transform duration-300 group-hover:translate-x-1"
                aria-hidden="true"
              >
                →
              </span>
            </button>
          </div>
        )}

        {!isLoading && !authRequired && error && (
          <ErrorState message={error} onRetry={reload} />
        )}

        {!isLoading && !authRequired && !error && places.length === 0 && (
          <EmptyState
            icon="🧭"
            title="Hələ istiqamət yoxdur"
            description="Yenidən cəhd et və ya bir az sonra qayıt."
            action={
              <button
                type="button"
                onClick={reload}
                className="rounded-full bg-ink-900 px-6 py-3 text-sm font-semibold text-white transition-all hover:bg-ink-800 active:scale-95"
              >
                Yenidən yüklə
              </button>
            }
          />
        )}

        {!isLoading && !authRequired && !error && places.length > 0 && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {places.map((place) => {
              const isFavorite = favorites.some((item) => item.id === place.id);
              const details = getDestinationDetails(place);

              return (
                <div
                  key={place.id}
                  className="group cursor-pointer overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-soft transition-all duration-300 hover:-translate-y-1.5 hover:shadow-lift focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                >
                  <div className="relative aspect-[16/10] w-full overflow-hidden bg-slate-200">
                    <DestinationImage
                      candidates={place.images}
                      src={place.imageUrl}
                      alt={place.title}
                      className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                      zoomable
                    />

                    <div
                      aria-hidden="true"
                      className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/25 via-transparent to-transparent"
                    />

                    <button
                      type="button"
                      onClick={(e) => handleToggleFavorite(e, place)}
                      aria-label={
                        isFavorite ? "Favoritlərdən sil" : "Favoritlərə əlavə et"
                      }
                      className={`absolute right-3 top-3 z-10 flex h-11 w-11 items-center justify-center rounded-full border border-white shadow-lg transition-all duration-300 active:scale-90 ${
                        isFavorite
                          ? "scale-105 bg-white text-red-500"
                          : "bg-white text-ink-500 hover:scale-110 hover:text-red-500"
                      }`}
                    >
                      {isFavorite ? (
                        <FaHeart className="h-5 w-5 transition-all duration-300" />
                      ) : (
                        <FaRegHeart className="h-5 w-5 transition-all duration-300" />
                      )}
                    </button>

                    {isFavorite && (
                      <span className="absolute left-3 top-3 rounded-full bg-ink-900/55 px-3 py-1.5 text-[11px] font-semibold text-white backdrop-blur-md">
                        Favorit
                      </span>
                    )}
                  </div>

                  <div className="p-5">
                    <h3 className="text-base font-bold leading-snug text-ink-900">
                      {place.title}
                    </h3>

                    <p className="mt-1 text-xs leading-relaxed text-ink-400">
                      {place.subtitle}
                    </p>

                    <p className="mt-3 line-clamp-2 text-xs leading-relaxed text-ink-500">
                      {details.headline}
                    </p>

                    <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3.5">
                      <span className="text-xs font-medium text-ink-400">
                        Daha ətraflı
                      </span>

                      <span
                        className="text-xs font-semibold text-brand-600 transition-transform duration-200 group-hover:translate-x-1"
                        aria-hidden="true"
                      >
                        →
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}

export default LocationsDiscovered;
