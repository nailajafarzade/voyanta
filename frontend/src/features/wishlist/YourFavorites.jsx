import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { FaHeart } from "react-icons/fa";

import { toggleFavorite } from "../../redux/wishlistThunks";
import DestinationImage from "../../components/common/DestinationImage";
import { getDestinationDetails } from "../../utils/destinationDetails";

function YourFavorites() {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const favorites = useSelector(
    (state) => state.wishlist.favorites
  );
  const isLoading = useSelector((state) => state.wishlist.isLoading);

  const handleRemoveFavorite = (e, place) => {
    e.stopPropagation();

    // Ürək = backend-dən silmək (DELETE /api/wishlist/{id})
    dispatch(toggleFavorite(place));
  };

  return (
    <section className="min-h-screen bg-canvas px-4 py-16 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-7xl">
        <div className="mb-10 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-ink-900 sm:text-4xl">
              Favorilərim
            </h1>

            <p className="mt-2 text-sm text-ink-400">
              Səyahət üçün yadda saxladığın yerlər burada görünəcək.
            </p>
          </div>

          {favorites.length > 0 && (
            <span className="w-fit rounded-full border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-ink-500">
              {favorites.length} favorit məkan
            </span>
          )}
        </div>

        {favorites.length === 0 && isLoading ? (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <div
                key={index}
                className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-soft"
              >
                <div className="voy-skeleton aspect-[16/10] w-full" />
                <div className="space-y-2.5 p-5">
                  <div className="voy-skeleton h-4 w-3/4 rounded-full" />
                  <div className="voy-skeleton h-3 w-1/2 rounded-full" />
                </div>
              </div>
            ))}
          </div>
        ) : favorites.length === 0 ? (
          <div
            className="
              min-h-[400px]
              bg-white
              rounded-3xl
              border
              border-slate-100
              shadow-sm
              flex
              flex-col
              items-center
              justify-center
              text-center
              px-6
            "
          >
            <div
              className="
                w-20
                h-20
                rounded-full
                bg-red-50
                flex
                items-center
                justify-center
                mb-5
              "
            >
              <FaHeart className="w-9 h-9 text-red-400" />
            </div>

            <h2 className="text-xl font-bold text-slate-900">
              Hələ favorit yerin yoxdur
            </h2>

            <p className="max-w-md mt-2 text-sm text-slate-400 leading-relaxed">
              Bəyəndiyin səyahət məkanlarını ürək işarəsinə klikləyərək burada
              saxlaya bilərsən.
            </p>

            <button
              type="button"
              onClick={() => navigate("/location")}
              className="
                mt-6
                px-6
                py-3
                rounded-full
                bg-slate-900
                text-white
                text-sm
                font-semibold
                hover:bg-slate-800
                transition-colors
                active:scale-95
              "
            >
              Yerlərə bax
            </button>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {favorites.map((place) => (
                <div
                  key={place.id}
                  className="
                    group
                    cursor-pointer
                    bg-white
                    rounded-3xl
                    overflow-hidden
                    border
                    border-slate-100
                    shadow-soft
                    hover:shadow-lift
                    hover:-translate-y-1
                    transition-all
                    duration-300
                    focus:outline-none
                    focus-visible:ring-2
                    focus-visible:ring-brand-500
                  "
                >
                  <div className="relative w-full aspect-[16/10] overflow-hidden bg-slate-200">
                    <DestinationImage
                      candidates={place.images}
                      src={place.imageUrl}
                      alt={place.title}
                      className="
                        w-full
                        h-full
                        object-cover
                        transition-transform
                        duration-500
                        group-hover:scale-105
                      "
                      zoomable
                    />

                    <div
                      className="
                        absolute
                        inset-0
                        bg-gradient-to-t
                        from-black/20
                        via-transparent
                        to-transparent
                        pointer-events-none
                      "
                    />

                    <button
                      type="button"
                      onClick={(e) =>
                        handleRemoveFavorite(e, place)
                      }
                      aria-label="Favoritdən sil"
                      className="
                        absolute
                        top-3
                        right-3
                        z-10
                        w-11
                        h-11
                        rounded-full
                        flex
                        items-center
                        justify-center
                        bg-white
                        border
                        border-white
                        text-red-500
                        shadow-lg
                        transition-all
                        duration-300
                        hover:scale-110
                        active:scale-90
                      "
                    >
                      <FaHeart className="w-5 h-5 fill-current" />
                    </button>

                    <span
                      className="
                        absolute
                        top-3
                        left-3
                        bg-black/50
                        backdrop-blur-md
                        text-white
                        text-[11px]
                        font-semibold
                        px-3
                        py-1.5
                        rounded-full
                      "
                    >
                      Favorit
                    </span>
                  </div>

                  <div className="p-5">
                    <h3 className="text-base font-bold text-slate-900 leading-snug">
                      {place.title}
                    </h3>

                    <p className="mt-1 text-xs text-slate-400 leading-relaxed">
                      {place.subtitle}
                    </p>

                    <p className="mt-3 line-clamp-2 text-xs leading-relaxed text-ink-500">
                      {getDestinationDetails(place).headline}
                    </p>

                    <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3.5">

                      <span
                        className="
                          text-xs
                          font-semibold
                          text-brand-600
                          group-hover:translate-x-1
                          transition-transform
                          duration-200
                        "
                        aria-hidden="true"
                      >
                        →
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </section>
  );
}

export default YourFavorites;