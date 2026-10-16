import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { FaRegHeart, FaHeart, FaArrowLeft, FaMapMarkerAlt } from "react-icons/fa";

import { getFeaturedDestinations } from "../../api/destinations";
import { toggleFavorite } from "../../redux/wishlistThunks";
import { toPlace } from "../../utils/destinations";
import { createImagePlan, getPlanImage } from "../../utils/imageAssignment";
import { getDestinationDetails } from "../../utils/destinationDetails";
import { getSeason, monthRange, seasonStatus } from "../../utils/season";
import { useAuth } from "../../context/authContext";
import { useAuthModal } from "../../context/authModalContext";
import { usePlanner } from "../../context/plannerContext";
import DestinationImage from "../../components/common/DestinationImage";
import Reveal from "../../components/common/Reveal";
import PostcardCard from "../../components/common/PostcardCard";
import { CardSkeleton, EmptyState } from "../../components/common/States";

/**
 * /destination/:destinationId — istiqamət haqqında ətraflı məlumat (PO #3).
 *
 * Şəbəkə tələbi yalnız mövcud `GET /destinations/featured`-dir — yeni endpoint
 * yaradılmır. Ətraflı məlumat (görüləcək yerlər, fəaliyyətlər, faydalı
 * məlumat) frontend-dəki curated məlumat bazasından gəlir.
 */
function DestinationDetailPage() {
  const { destinationId } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { isAuthenticated } = useAuth();
  const { openLogin } = useAuthModal();
  const { openPlanner } = usePlanner();

  const favorites = useSelector((state) => state.wishlist.favorites);

  const [place, setPlace] = useState(null);
  const [others, setOthers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  // Səhifə daxilində də təkrar qaçınmaq üçün plan: hero ən yaxşı şəkli alır,
  // "Digər istiqamətlər" isə eyni istiqamət olsa fərqli namizəd göstərir.
  const imagePlan = useMemo(
    () =>
      createImagePlan([
        { key: "detail-hero", places: place ? [place] : [] },
        { key: "detail-others", places: others },
      ]),
    [place, others]
  );

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError(null);
    setPlace(null);

    (async () => {
      try {
        const destinations = await getFeaturedDestinations();
        if (cancelled) return;

        const places = (destinations ?? []).map(toPlace);
        const matched = places.find((item) => String(item.id) === String(destinationId));

        if (matched) {
          setPlace(matched);
          setOthers(places.filter((item) => String(item.id) !== String(destinationId)));
        } else {
          setError("Bu istiqamət tapılmadı.");
        }
      } catch (caught) {
        if (!cancelled) {
          setError("İstiqamət məlumatı yüklənmədi. Bir az sonra yenidən cəhd et.");
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [destinationId, reloadKey]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-canvas pt-28 pb-24">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="voy-skeleton h-72 w-full rounded-[32px]" />
          <CardSkeleton count={3} className="mt-10 lg:grid-cols-3" />
        </div>
      </div>
    );
  }

  if (error || !place) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas px-4 pt-28">
        <div className="w-full max-w-lg">
          <EmptyState
            icon="🧭"
            title="İstiqamət tapılmadı"
            description={error ?? "Bu istiqamət artıq mövcud deyil."}
            action={
              <div className="flex flex-wrap justify-center gap-3">
                <button
                  type="button"
                  onClick={() => navigate("/")}
                  className="rounded-full bg-ink-900 px-6 py-3 text-sm font-semibold text-white transition-all hover:bg-ink-800 active:scale-95"
                >
                  Ana səhifə
                </button>
                <button
                  type="button"
                  onClick={() => setReloadKey((k) => k + 1)}
                  className="rounded-full border border-slate-200 bg-white px-6 py-3 text-sm font-semibold text-ink-700 transition-all hover:bg-slate-50 active:scale-95"
                >
                  Yenidən cəhd et
                </button>
              </div>
            }
          />
        </div>
      </div>
    );
  }

  return (
    <DestinationDetailContent
      place={place}
      others={others}
      isFavorite={favorites.some((item) => item.id === place.id)}
      isAuthenticated={isAuthenticated}
      onToggleFavorite={() => {
        if (!isAuthenticated) {
          openLogin();
          return;
        }
        dispatch(toggleFavorite(place));
      }}
      onPlan={openPlanner}
    />
  );
}

export default DestinationDetailPage;

/** "Dili — Yunan dili" sətri. */
function InfoRow({ label, value }) {
  if (!value) return null;

  return (
    <div className="flex items-start justify-between gap-4">
      <dt className="shrink-0 text-xs font-medium text-ink-400">{label}</dt>
      <dd className="text-right text-xs font-bold text-ink-900">{value}</dd>
    </div>
  );
}

/** Səhifənin vizual hissəsi. */
function DestinationDetailContent({
  place,
  others,
  isFavorite,
  isAuthenticated,
  onToggleFavorite,
  onPlan,
}) {
  const details = getDestinationDetails(place);
  const season = getSeason();
  const status = seasonStatus(details.bestMonths);

  return (
    <div className="bg-canvas pb-24">
      {/* ================= HERO ================= */}
      <section className="relative h-[52vh] min-h-[380px] w-full overflow-hidden sm:h-[58vh]">
        <DestinationImage
          image={getPlanImage(imagePlan, "detail-hero", place)}
          candidates={place.images}
          src={place.imageUrl}
          alt={place.title}
          className="absolute inset-0 h-full w-full"
          season
          eager
          zoomable
        />
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-ink-900 via-ink-900/45 to-ink-900/10" />

        <div className="relative mx-auto flex h-full max-w-7xl flex-col justify-between px-4 py-6 sm:px-6 lg:px-8">
          {/* yuxarı: geri + fav */}
          <div className="flex items-center justify-between">
            <Link
              to="/"
              className="inline-flex items-center gap-2 rounded-full bg-white/15 px-4 py-2.5 text-sm font-semibold text-white backdrop-blur-md transition-all duration-300 hover:bg-white/25 active:scale-95"
            >
              <FaArrowLeft className="h-3.5 w-3.5" />
              <span>Geriyə</span>
            </Link>

            <button
              type="button"
              onClick={onToggleFavorite}
              aria-label={isFavorite ? "Favoritlərdən sil" : "Favoritlərə əlavə et"}
              className={`flex h-11 w-11 items-center justify-center rounded-full backdrop-blur-md transition-all duration-300 active:scale-90 ${
                isFavorite
                  ? "bg-white text-red-500"
                  : "bg-white/15 text-white hover:bg-white/25"
              }`}
            >
              {isFavorite ? (
                <FaHeart className="h-5 w-5" />
              ) : (
                <FaRegHeart className="h-5 w-5" />
              )}
            </button>
          </div>

          {/* alt: başlıq */}
          <div className="pb-8">
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-white backdrop-blur-md">
                <FaMapMarkerAlt className="h-3 w-3" />
                {place.subtitle}
              </span>

              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1.5 text-[11px] font-semibold text-white backdrop-blur-md">
                <span aria-hidden="true">{season.icon}</span>
                {season.label}
              </span>

              {status.state === "peak" && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-mint-500/90 px-3 py-1.5 text-[11px] font-bold text-white">
                  <span aria-hidden="true">✨</span>
                  İndi ən yaxşı vaxtdır
                </span>
              )}
            </div>

            <h1 className="max-w-3xl text-3xl font-extrabold leading-[1.1] tracking-tight text-white drop-shadow-sm sm:text-5xl lg:text-6xl">
              {place.title}
            </h1>

            <p className="mt-4 max-w-2xl text-base leading-relaxed text-white/85 sm:text-lg">
              {details.headline}
            </p>
          </div>
        </div>
      </section>

      {/* ================= MÜHAZİRƏ ================= */}
      <section className="relative z-10 mx-auto -mt-10 max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          {details.facts.map((fact, index) => (
            <Reveal
              key={fact.label}
              delay={index * 80}
              className="rounded-2xl border border-slate-100 bg-white p-5 shadow-soft"
            >
              <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-400">
                {fact.label}
              </p>
              <p className="mt-1.5 text-sm font-bold text-ink-900">{fact.value}</p>
            </Reveal>
          ))}
        </div>
      </section>

      {/* ================= ƏSAS MƏZMUN ================= */}
      <div className="mx-auto max-w-7xl px-4 pt-16 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-10 lg:grid-cols-3">
          {/* Sol sütun: yerlər + fəaliyyətlər */}
          <div className="lg:col-span-2">
            <Reveal>
              <h2 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">
                Görməyə dəyər yerlər
              </h2>
              <p className="mt-2 text-sm text-ink-500 sm:text-base">
                Bu istiqamətdə ən çox ziyarət edilən nöqtələr.
              </p>
            </Reveal>

            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
              {details.places.map((item, index) => (
                <Reveal
                  key={item.name}
                  delay={index * 70}
                  className="group flex gap-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-soft transition-all duration-300 hover:-translate-y-0.5 hover:shadow-card"
                >
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-base font-bold text-brand-600 transition-colors duration-300 group-hover:bg-brand-500 group-hover:text-white">
                    {index + 1}
                  </span>
                  <div>
                    <h3 className="text-sm font-bold text-ink-900">{item.name}</h3>
                    <p className="mt-1 text-xs leading-relaxed text-ink-500">
                      {item.note}
                    </p>
                  </div>
                </Reveal>
              ))}
            </div>

            {/* Fəaliyyətlər */}
            <Reveal className="mt-12">
              <h2 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">
                Fəaliyyətlər
              </h2>
              <p className="mt-2 text-sm text-ink-500 sm:text-base">
                Burada edə biləcəyin əsas şeylər.
              </p>
            </Reveal>

            <div className="mt-6 flex flex-wrap gap-3">
              {details.activities.map((activity, index) => (
                <Reveal
                  key={activity.label}
                  delay={index * 50}
                  className="inline-flex items-center gap-2.5 rounded-2xl border border-slate-100 bg-white px-4 py-3 text-sm font-semibold text-ink-700 shadow-soft transition-all duration-300 hover:-translate-y-0.5 hover:border-brand-200 hover:text-brand-700 hover:shadow-card"
                >
                  <span className="text-base" aria-hidden="true">
                    {activity.icon}
                  </span>
                  {activity.label}
                </Reveal>
              ))}
            </div>
          </div>

          {/* Sağ sütun: faydalı məlumat + CTA */}
          <aside className="lg:col-span-1">
            <div className="lg:sticky lg:top-28">
              <Reveal variant="right">
                <div className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-soft">
                  <div className="border-b border-slate-100 bg-gradient-to-r from-brand-50 to-mint-50 px-6 py-5">
                    <h2 className="text-base font-bold text-ink-900">
                      Faydalı məlumat
                    </h2>
                  </div>

                  <dl className="space-y-4 p-6">
                    <InfoRow label="Dili" value={details.language} />
                    <InfoRow label="Valyuta" value={details.currency} />
                    <InfoRow label="Tövsiyə olunan gün" value={details.stayLength} />
                    <InfoRow label="Xərc səviyyəsi" value={details.budget} />
                    {details.timezone && (
                      <InfoRow label="Saat zolağı" value={details.timezone} />
                    )}
                    {details.bestSeason && (
                      <InfoRow label="Mövsüm" value={details.bestSeason} />
                    )}
                    {details.bestMonths && (
                      <InfoRow
                        label="Ən yaxşı aylar"
                        value={monthRange(details.bestMonths)}
                      />
                    )}
                  </dl>
                </div>
              </Reveal>

              {/* CTA */}
              <Reveal variant="right" delay={100}>
                <div className="mt-6 overflow-hidden rounded-3xl bg-gradient-to-br from-ink-900 via-ink-800 to-brand-900 p-6 shadow-lift">
                  <span className="text-2xl" aria-hidden="true">
                    ✨
                  </span>
                  <h3 className="mt-3 text-lg font-bold leading-snug text-white">
                    {place.title} üçün səyahət planı hazırla
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-white/65">
                    7 qısa sual cavabla AI sənə uyğun gündəlik marşrut qursun.
                  </p>

                  <button
                    type="button"
                    onClick={onPlan}
                    className="mt-5 w-full rounded-full bg-white px-6 py-3 text-sm font-bold text-ink-900 transition-all duration-200 hover:bg-brand-50 active:scale-[0.97]"
                  >
                    Planlaşdırmaya başla
                  </button>

                  {!isAuthenticated && (
                    <p className="mt-3 text-center text-[11px] text-white/45">
                      Qeydiyyat tələb olunmur
                    </p>
                  )}
                </div>
              </Reveal>

              {/* Məsləhətlər */}
              {details.tips.length > 0 && (
                <Reveal variant="right" delay={180}>
                  <div className="mt-6 rounded-3xl border border-sun-200 bg-sun-50/70 p-6">
                    <h3 className="text-sm font-bold text-ink-900">
                      Yerləşdirmədə məsləhət
                    </h3>
                    <ul className="mt-3 space-y-2.5">
                      {details.tips.map((tip) => (
                        <li
                          key={tip}
                          className="flex gap-2.5 text-xs leading-relaxed text-ink-600"
                        >
                          <span className="mt-0.5 shrink-0" aria-hidden="true">
                            💡
                          </span>
                          <span>{tip}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </Reveal>
              )}
            </div>
          </aside>
        </div>

        {/* ================= OXŞAR İSTİQAMƏTLƏR ================= */}
        {others.length > 0 && (
          <section className="mt-20">
            <Reveal>
              <h2 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">
                Digər istiqamətlər
              </h2>
              <p className="mt-2 text-sm text-ink-500 sm:text-base">
                Bunun da baxa biləcəyin yerlər.
              </p>
            </Reveal>

            <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {others.slice(0, 3).map((item, index) => (
                <Reveal key={item.id} delay={index * 90}>
                  <PostcardCard
                    place={item}
                    image={getPlanImage(imagePlan, "detail-others", item)}
                    zoomable
                    onPlan={onPlan}
                  />
                </Reveal>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

