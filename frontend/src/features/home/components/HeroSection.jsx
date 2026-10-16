import { useEffect, useMemo, useState } from "react";
import { getHomepageStats } from "../../../api/homepage";
import { formatCount, formatRating } from "../../../utils/format";
import { getSeason } from "../../../utils/season";
import Reveal from "../../../components/common/Reveal";
import { usePlanner } from "../../../context/plannerContext";

import img1 from "../../../assets/srilanka.jpg";
import img2 from "../../../assets/russia.jpg";
import img3 from "../../../assets/poland.jpg";
import img4 from "../../../assets/australia.jpg";

// Hero kolajındakı 4 sabit istiqamət (lokal şəkillər)
const HERO_DESTINATIONS = [
  {
    id: "sri-lanka",
    image: img1,
    subtitle: "Təbiət və mədəniyyət",
    title: "Şri-Lanka",
  },
  {
    id: "russia",
    image: img2,
    subtitle: "Tarix və memarlıq",
    title: "Rusiya",
  },
  {
    id: "poland",
    image: img3,
    subtitle: "Köhnə şəhərlər",
    title: "Polşa",
  },
  {
    id: "australia",
    image: img4,
    subtitle: "Okean və təbiət",
    title: "Avstraliya",
  },
];

/**
 * Ana səhifənin hero bölməsi. Dizayn dili (mavi #5B8DEF, statistika bloku,
 * 4 istiqamət kolajı) saxlanılır. Kolaj lokal şəkillərdən qurulur.
 */
function HeroSection() {
  const { openPlanner } = usePlanner();
  const [stats, setStats] = useState(null);
  const [offset, setOffset] = useState(0);
  const season = useMemo(() => getSeason(), []);

  // GET /api/homepage/stats (açıq endpoint). Xəta olsa rəqəmlərin yerində "—" qalır.
  useEffect(() => {
    let cancelled = false;

    getHomepageStats()
        .then((loaded) => {
          if (!cancelled) setStats(loaded);
        })
        .catch((error) => {
          console.error("Ana səhifə statistikası yüklənmədi:", error);
        });

    return () => {
      cancelled = true;
    };
  }, []);

  // Yüngül parallax — yalnız hərəkət azaltma olmayan mühitdə
  useEffect(() => {
    const prefersReduced = window.matchMedia?.(
        "(prefers-reduced-motion: reduce)"
    ).matches;
    if (prefersReduced) return undefined;

    let frame = 0;
    const onScroll = () => {
      if (frame) return;
      frame = window.requestAnimationFrame(() => {
        frame = 0;
        setOffset(Math.min(window.scrollY, 320));
      });
    };

    window.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", onScroll);
      if (frame) window.cancelAnimationFrame(frame);
    };
  }, []);

  return (
      <div className="relative overflow-hidden bg-canvas py-16 font-sans antialiased selection:bg-brand-100 sm:py-20 lg:py-24">
        {/* arxa plan işıqları */}
        <div
            aria-hidden="true"
            className="pointer-events-none absolute -left-32 -top-28 h-[440px] w-[440px] rounded-full bg-brand-200/40 blur-3xl animate-voy-drift"
        />
        <div
            aria-hidden="true"
            className="pointer-events-none absolute -right-28 top-16 h-[400px] w-[400px] rounded-full bg-mint-200/40 blur-3xl animate-voy-float-slow"
        />
        <div
            aria-hidden="true"
            className="pointer-events-none absolute bottom-0 left-1/3 h-[280px] w-[280px] rounded-full bg-sun-200/25 blur-3xl animate-voy-float"
        />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 items-center gap-14 lg:grid-cols-12 lg:gap-8">
            {/* ============ SOL: mətn bloku ============ */}
            <div className="flex flex-col items-start space-y-6 lg:col-span-7">
              <Reveal>
                <div className="inline-flex items-center gap-1.5 rounded-full border border-brand-100 bg-brand-50 px-3.5 py-1.5 text-xs font-semibold tracking-wide text-brand-600">
                  <span aria-hidden="true">✨</span>
                  <span>Süni intellektlə səyahət planlaması</span>
                </div>
              </Reveal>

              <Reveal delay={70}>
              <span className="inline-flex items-center gap-2 rounded-full border border-mint-200 bg-white/80 px-4 py-2 text-xs font-semibold text-ink-700 shadow-soft backdrop-blur-sm">
                <span className="relative flex h-2 w-2">
                  <span className="absolute inline-flex h-full w-full rounded-full bg-mint-500 opacity-60 animate-voy-pulse-ring" />
                  <span className="relative inline-flex h-2 w-2 rounded-full bg-mint-500" />
                </span>
                <span aria-hidden="true">{season.icon}</span>
                <span>{season.label} mövsümü üçün istiqamətlər</span>
              </span>
              </Reveal>

              <Reveal delay={140}>
                <h1 className="text-4xl font-extrabold leading-[1.1] tracking-tight text-ink-900 sm:text-5xl lg:text-6xl">
                  Növbəti səyahətini{" "}
                  <span className="relative inline-block">
                  <span className="relative z-10 bg-gradient-to-r from-brand-500 via-brand-600 to-mint-500 bg-clip-text text-transparent">
                    bir neçə sualla
                  </span>
                  <svg
                      className="absolute inset-x-0 bottom-0 z-0 h-2.5 w-full text-sun-400/70"
                      viewBox="0 0 200 12"
                      fill="none"
                      preserveAspectRatio="none"
                      aria-hidden="true"
                  >
                    <path
                        d="M2 8.5C50 3 150 3 198 7"
                        stroke="currentColor"
                        strokeWidth="3.5"
                        strokeLinecap="round"
                    />
                  </svg>
                </span>{" "}
                  planla
                </h1>
              </Reveal>

              <Reveal delay={210}>
                <p className="max-w-xl text-base leading-relaxed text-ink-500 sm:text-lg">
                  Tur şirkətlərini axtarmağa vaxt itirmə. Marağını, kiminlə
                  getdiyini və büdcəni de, süni intellekt sənə uyğun plan
                  hazırlasın.
                </p>
              </Reveal>

              <Reveal delay={280}>
                <div className="flex w-full flex-col items-start gap-4 pt-2 sm:w-auto sm:flex-row sm:items-center">
                  <a
                      href="#start-planning"
                      onClick={(event) => {
                        event.preventDefault();
                        openPlanner();
                      }}
                      className="group relative flex w-full items-center justify-center gap-2 overflow-hidden rounded-full bg-brand-500 px-7 py-3.5 text-base font-semibold text-white shadow-glow transition-all duration-300 hover:bg-brand-600 active:scale-[0.97] sm:w-auto"
                  >
                    <span className="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/25 to-transparent transition-transform duration-700 group-hover:translate-x-full" />
                    <span className="relative">Planlaşdırmaya başla</span>
                    <span className="relative text-lg transition-transform duration-300 group-hover:translate-x-1">
                    →
                  </span>
                  </a>

                  <span className="text-xs font-normal text-ink-400 sm:text-sm">
                  Qeydiyyat tələb olunmur, ilk planı pulsuz gör
                </span>
                </div>
              </Reveal>

              <Reveal delay={350}>
                <dl className="grid w-full max-w-md grid-cols-2 gap-8 border-t border-slate-200/80 pt-6">
                  <div>
                    <dt className="sr-only">Hazırlanan plan sayı</dt>
                    <dd className="text-2xl font-bold text-ink-900 sm:text-3xl">
                      {formatCount(stats?.plansCreated)}
                    </dd>
                    <p className="mt-0.5 text-xs text-ink-500 sm:text-sm">
                      hazırlanan səyahət planı
                    </p>
                  </div>

                  <div>
                    <dt className="sr-only">İstifadəçi məmnuniyyəti</dt>
                    <dd className="text-2xl font-bold text-ink-900 sm:text-3xl">
                      {formatRating(stats?.avgRating)}
                    </dd>
                    <p className="mt-0.5 text-xs text-ink-500 sm:text-sm">
                      istifadəçi məmnuniyyəti
                    </p>
                  </div>
                </dl>
              </Reveal>
            </div>

            {/* ============ SAĞ: istiqamət kolajı ============ */}
            <Reveal variant="zoom" delay={200} className="lg:col-span-5">
              <div className="relative w-full">
                <div
                    className="grid grid-cols-2 gap-3 sm:gap-4"
                    style={{ transform: `translateY(${offset * -0.035}px)` }}
                >
                  {HERO_DESTINATIONS.map((item, index) => (
                      <div
                          key={item.id}
                          className="group relative aspect-[4/3] overflow-hidden rounded-2xl bg-slate-200 shadow-soft ring-1 ring-white/60 transition-all duration-500 hover:shadow-lift"
                          style={{
                            animation: `voy-fade-up 0.7s var(--ease-out-expo) ${index * 0.12}s both`,
                          }}
                      >
                        <img
                            src={item.image}
                            alt={item.title}
                            loading={index === 0 ? "eager" : "lazy"}
                            decoding="async"
                            draggable="false"
                            className="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-110"
                        />

                        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />

                        <div className="absolute inset-x-0 bottom-0 flex items-end justify-between gap-2 p-3.5 sm:p-4">
                          <div>
                            <p className="text-[9px] font-bold uppercase tracking-[0.15em] text-white/70">
                              {item.subtitle}
                            </p>
                            <p className="mt-0.5 text-xs font-medium tracking-wide text-white drop-shadow-sm sm:text-sm">
                              {item.title}
                            </p>
                          </div>

                          <span
                              aria-hidden="true"
                              className="translate-x-1 text-sm text-white opacity-0 transition-all duration-300 group-hover:translate-x-0 group-hover:opacity-100"
                          >
                        ↗
                      </span>
                        </div>
                      </div>
                  ))}
                </div>

                {/* üzərində üzən "mövsüm" etiketi */}
                <div className="absolute -left-3 top-8 hidden animate-voy-float-slow rounded-2xl border border-slate-100 bg-white/95 p-4 shadow-lift backdrop-blur-md sm:block lg:-left-6">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-ink-400">
                    Mövsüm
                  </p>
                  <p className="mt-1 flex items-center gap-1.5 text-sm font-bold text-ink-900">
                    <span aria-hidden="true">{season.icon}</span>
                    {season.label}
                  </p>
                </div>

                {/* üzərində üzən "trendlər" etiketi */}
                <div
                    className="absolute -bottom-5 right-2 hidden animate-voy-float rounded-2xl border border-slate-100 bg-white/95 px-5 py-4 shadow-lift backdrop-blur-md sm:block lg:-right-5"
                    style={{ animationDelay: "1.4s" }}
                >
                  <p className="text-[10px] font-bold uppercase tracking-wider text-ink-400">
                  <span className="mr-1" aria-hidden="true">
                    🔥
                  </span>
                    Trendlər
                  </p>
                  <p className="mt-1 text-sm font-bold text-ink-900">
                    Dəniz məkanları
                  </p>
                </div>
              </div>
            </Reveal>
          </div>
        </div>
      </div>
  );
}

export default HeroSection;