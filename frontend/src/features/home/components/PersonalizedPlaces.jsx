import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { FaHeart, FaRegHeart } from "react-icons/fa";
import { toggleFavorite } from "../../../redux/wishlistThunks";
import { usePlanner } from "../../../context/plannerContext";
import { useAuth } from "../../../context/authContext";
import { useAuthModal } from "../../../context/authModalContext";
import Reveal from "../../../components/common/Reveal";
import { useBackendPlaces } from "../../../hooks/useBackendPlaces";
import { PERSONALIZED_JOURNEY } from "../../../data/personalizedJourney";
import "./PersonalizedPlaces.css";


/**
 * "SƏNƏ ÖZƏL SƏYAHƏT MARŞRUTU" — 100% lokal, frontend-only.
 * 11 istiqamət, eyni anda 4-ü görünür, ~7.5 saniyədən bir sakit keçid.
 * Heç bir API sorğusu yoxdur. Bütün mətnlər Azərbaycanca.
 * Kartları görmək üçün daxil olmaq tələb olunur.
 */
const ROTATE_MS = 7500;
const EXIT_MS = 750;
const VISIBLE_COUNT = 4;

function shuffledQueue(list) {
  const arr = [...list];
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

function PersonalizedPlaces() {
  const planner = usePlanner();
  const openPlanner = planner?.openPlanner;
  const { isAuthenticated, isLoading } = useAuth();
  const { openLogin } = useAuthModal();
  const isLocked = !isLoading && !isAuthenticated;

  const dispatch = useDispatch();
  const favorites = useSelector((state) => state.wishlist.favorites);

  // Staqik slug (məs. "tromso") → backend-in real `place`-i (UUID).
  // Mövcud `useBackendPlaces` rezolyutoru — LovedPlaceCard /
  // SeasonDestinationCard ilə EYNİ mexanizm. Backend-də belə sətir
  // yoxdursa heç bir saxta ID göndərilmir (favorit mümkün deyil).
  const { resolveBackendPlace } = useBackendPlaces();
  const backendPlaceOf = (dest) => resolveBackendPlace(dest?.city);

  const isFavorite = (dest) => {
    const backendPlace = backendPlaceOf(dest);
    return (
      Boolean(backendPlace?.id) &&
      favorites.some((item) => item.id === backendPlace.id)
    );
  };

  const handleHeart = (e, dest) => {
    e.stopPropagation();
    if (!isAuthenticated) {
      openLogin();
      return;
    }
    const backendPlace = backendPlaceOf(dest);
    if (!backendPlace) return;
    dispatch(toggleFavorite(backendPlace));
  };

  const [queue, setQueue] = useState(() => shuffledQueue(PERSONALIZED_JOURNEY));
  const [start, setStart] = useState(0);
  const [isLeaving, setIsLeaving] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [selected, setSelected] = useState(null);
  const timers = useRef({ tick: null, swap: null });

  const visible = useMemo(() => {
    const out = [];
    for (let i = 0; i < VISIBLE_COUNT; i += 1) out.push(queue[(start + i) % queue.length]);
    return out;
  }, [queue, start]);

  const advance = useCallback(() => {
    setIsLeaving(true);
    timers.current.swap = window.setTimeout(() => {
      setStart((prev) => {
        const next = (prev + VISIBLE_COUNT) % queue.length;
        if (next === 0) setQueue(shuffledQueue(PERSONALIZED_JOURNEY));
        return next;
      });
      setIsLeaving(false);
    }, EXIT_MS);
  }, [queue.length]);

  useEffect(() => {
    if (typeof window === "undefined") return undefined;
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return undefined;
    if (isPaused || selected || !isAuthenticated) return undefined;
    const activeTimers = timers.current;
    activeTimers.tick = window.setTimeout(advance, ROTATE_MS);
    return () => {
      window.clearTimeout(activeTimers.tick);
      window.clearTimeout(activeTimers.swap);
    };
  }, [advance, isPaused, isAuthenticated, selected, start, queue]);

  useEffect(() => () => {
    window.clearTimeout(timers.current.tick);
    window.clearTimeout(timers.current.swap);
  }, []);


  const openDetails = useCallback((dest) => {
    if (!dest) return;
    if (!isAuthenticated) {
      openLogin();
      return;
    }
    setSelected(dest);
  }, [isAuthenticated, openLogin]);

  useEffect(() => {
    if (!selected) return undefined;
    const onKey = (e) => { if (e.key === "Escape") setSelected(null); };
    window.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { window.removeEventListener("keydown", onKey); document.body.style.overflow = prev; };
  }, [selected]);

  // Token bərpa olunarkən kilid ekranı yanıb-sönməsin
  if (isLoading) return null;

  return (
      <section id="sene-ozel-marsrut" aria-labelledby="journey-title" className="voy-journey py-20 lg:py-28">
        <svg className="voy-journey-route hidden lg:block" viewBox="0 0 1200 120" fill="none" preserveAspectRatio="none" aria-hidden="true">
          <path d="M-20 78 C 180 20, 320 108, 520 62 S 860 18, 1220 72" stroke="rgba(255,255,255,0.22)" strokeWidth="1.6" strokeDasharray="2 9" strokeLinecap="round" />
        </svg>

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col items-start justify-between gap-6 lg:flex-row lg:items-end">
            <div className="max-w-2xl">
              <Reveal className="mb-3 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-[0.12em]">
                <span className="text-sun-300"><span className="mr-1">✦</span>Sənə özəl kəşf</span>
              </Reveal>
              <Reveal delay={70}><h2 id="journey-title" className="text-3xl font-extrabold leading-[1.15] tracking-tight text-white sm:text-4xl lg:text-[2.65rem]">Seçilmiş yerlər</h2></Reveal>
            </div>
            {isAuthenticated && (
                <Reveal delay={200} className="flex shrink-0 items-center gap-3">
                  <button type="button" onClick={() => setIsPaused((p) => !p)} aria-pressed={isPaused} aria-label={isPaused ? "Avtomatik keçidi davam etdir" : "Avtomatik keçidi dayandır"} className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-white/15 bg-white/5 text-base text-white/80 transition-all duration-300 hover:bg-white/10 active:scale-95">{isPaused ? "▶" : "⏸"}</button>
                  {openPlanner && (<button type="button" onClick={openPlanner} className="group inline-flex items-center gap-2 rounded-full bg-white px-6 py-3.5 text-sm font-semibold text-ink-900 transition-all duration-300 hover:bg-brand-50 active:scale-95"><span>Öz planımı yarat</span><span className="transition-transform duration-300 group-hover:translate-x-1" aria-hidden="true">→</span></button>)}
                </Reveal>
            )}
          </div>
          {/* GRID2 */}
          <Reveal delay={120}>
            <div className="relative mt-12">
              <div
                  className={`voy-journey-grid ${isLeaving ? "is-leaving" : ""} ${isLocked ? "pointer-events-none select-none blur-md" : ""}`}
                  role="list"
                  aria-label="Sənə özəl istiqamətlər"
                  aria-hidden={isLocked || undefined}
              >
                {visible.map((dest, i) => (
                    <article key={`${dest.id}-${start}-${i}`} role="listitem" className={`voy-journey-card voy-stop-${i} relative`}>
                      <button
                          type="button"
                          tabIndex={isLocked ? -1 : 0}
                          onClick={(e) => handleHeart(e, dest)}
                          aria-pressed={isFavorite(dest)}
                          aria-disabled={!backendPlaceOf(dest) || undefined}
                          aria-label={isFavorite(dest) ? "Favoritdən sil" : "Favoritlərə əlavə et"}
                          title={backendPlaceOf(dest) ? undefined : "Bu yer üçün mövcud deyil"}
                          className={`absolute right-4 top-4 z-20 flex h-11 w-11 items-center justify-center rounded-full border shadow-lg transition-all duration-300 hover:scale-110 active:scale-90 ${
                              isFavorite(dest)
                                  ? "border-white bg-white text-red-500"
                                  : "border-white/30 bg-ink-900/55 text-white backdrop-blur-md hover:bg-ink-900/75"
                          } ${backendPlaceOf(dest) ? "" : "cursor-not-allowed opacity-60"}`}
                      >
                        {isFavorite(dest) ? <FaHeart className="h-5 w-5" /> : <FaRegHeart className="h-5 w-5" />}
                      </button>
                      <span className="voy-journey-node hidden lg:flex" aria-hidden="true" />
                      <button type="button" tabIndex={isLocked ? -1 : 0} onClick={() => openDetails(dest)} aria-label={`${dest.city}, ${dest.country} — ${dest.headline}`} className="voy-journey-frame group">
                        <img src={dest.image} alt={dest.alt} loading={i === 0 ? "eager" : "lazy"} decoding="async" draggable="false" />
                        <span className="voy-journey-shade" aria-hidden="true" />
                        <span className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-ink-900/55 px-3 py-1.5 text-[10px] font-bold uppercase tracking-[0.12em] text-white/90" aria-hidden="true"><span className="text-sun-400">✦</span>Dayanacaq 0{i + 1}</span>
                        <span className="absolute inset-x-0 bottom-0 block p-5 sm:p-6">
                      <span className="voy-journey-default block">
                        <span className="block text-[11px] font-bold uppercase tracking-[0.16em] text-white/70">{dest.country}</span>
                        <span className="mt-1 block text-2xl font-extrabold tracking-tight text-white">{dest.city}</span>
                      </span>
                      <span className="voy-journey-hover mt-1 block">
                        <span className="block text-lg font-extrabold leading-snug text-white">{dest.headline}</span>
                        <span className="mt-2 block text-sm leading-relaxed text-white/75">{dest.description}</span>
                        <span className="mt-3 flex flex-wrap gap-1.5">{dest.tags.map((t) => (<span key={t} className="rounded-full border border-white/15 bg-white/10 px-2.5 py-1 text-[10px] font-bold text-white/90">{t}</span>))}</span>
                        <span className="mt-4 inline-flex items-center gap-2 text-sm font-bold text-sun-300">Daha çox kəşf et <span aria-hidden="true">→</span></span>
                      </span>
                    </span>
                      </button>
                    </article>
                ))}
              </div>

              {isLocked && (
                  <div className="absolute inset-0 z-10 flex items-center justify-center px-4">
                    <div className="w-full max-w-sm rounded-4xl border border-white/10 bg-white p-8 text-center shadow-soft">
                      <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-brand-50 text-3xl">
                        🔒
                      </div>
                      <h3 className="text-xl font-extrabold tracking-tight text-ink-900">
                        Sənə özəl marşrutu gör
                      </h3>
                      <p className="mt-2 text-sm leading-relaxed text-ink-500">
                        Sənin üçün seçilmiş istiqamətləri görmək üçün Google hesabınla daxil ol.
                      </p>
                      <button
                          type="button"
                          onClick={openLogin}
                          className="mt-7 w-full rounded-full bg-brand-500 px-7 py-3.5 text-sm font-semibold text-white shadow-glow transition-all duration-300 hover:bg-brand-600 active:scale-95"
                      >
                        Daxil ol
                      </button>
                    </div>
                  </div>
              )}
            </div>
          </Reveal>
          {isAuthenticated && (
              <p className="mt-6 text-center text-xs text-white/40 lg:hidden">Kartı seç — sənin üçün nə hazırladığımızı gör</p>
          )}

          {selected && (
              <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink-900/60 p-0 backdrop-blur-sm sm:items-center sm:p-6" role="dialog" aria-modal="true" aria-label={`${selected.city} ətraflı məlumat`} onClick={() => setSelected(null)}>
                <div className="relative max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-t-3xl bg-white shadow-lift sm:rounded-3xl" onClick={(e) => e.stopPropagation()}>
                  <div className="relative aspect-[16/10] w-full overflow-hidden bg-slate-200 sm:rounded-t-3xl">
                    <img src={selected.image} alt={selected.alt} className="absolute inset-0 h-full w-full object-cover" />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/5 to-transparent" />
                    <button
                        type="button"
                        onClick={(e) => handleHeart(e, selected)}
                        aria-pressed={isFavorite(selected)}
                        aria-disabled={!backendPlaceOf(selected) || undefined}
                        aria-label={isFavorite(selected) ? "Favoritdən sil" : "Favoritlərə əlavə et"}
                        title={backendPlaceOf(selected) ? undefined : "Bu yer üçün mövcud deyil"}
                        className={`absolute right-16 top-4 flex h-10 w-10 items-center justify-center rounded-full transition-all duration-300 active:scale-90 ${
                            isFavorite(selected) ? "bg-white text-red-500" : "bg-ink-900/55 text-white"
                        } ${backendPlaceOf(selected) ? "" : "cursor-not-allowed opacity-60"}`}
                    >
                      {isFavorite(selected) ? <FaHeart className="h-5 w-5" /> : <FaRegHeart className="h-5 w-5" />}
                    </button>
                    <button type="button" onClick={() => setSelected(null)} aria-label="Bağla" autoFocus className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-ink-900/55 text-lg font-bold text-white">✕</button>
                    <div className="absolute inset-x-0 bottom-0 p-5">
                      <p className="text-[11px] font-bold uppercase tracking-[0.15em] text-white/75">{selected.country}</p>
                      <h3 className="mt-1 text-2xl font-extrabold text-white">{selected.city}</h3>
                    </div>
                  </div>
                  <div className="space-y-5 p-5 sm:p-6">
                    <p className="text-lg font-extrabold leading-snug text-ink-900">{selected.headline}</p>
                    <p className="text-sm leading-relaxed text-ink-600">{selected.description}</p>
                    <div className="flex flex-wrap gap-2">{selected.tags.map((t) => (<span key={t} className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-bold text-ink-700">{t}</span>))}</div>
                    {openPlanner && (<button type="button" onClick={() => { setSelected(null); openPlanner(); }} className="w-full rounded-full bg-ink-900 px-6 py-3.5 text-sm font-bold text-white">Bu ruhda plan yarat →</button>)}
                  </div>
                </div>
              </div>
          )}
        </div>
      </section>
  );
}

export default PersonalizedPlaces;