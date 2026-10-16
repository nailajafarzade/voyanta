import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import star from "../../assets/star.png";
import coffe from "../../assets/coffe.png";
import { getPlanStatus } from "../../api/plans";
import { apiErrorMessage } from "../../api/errors";

const POLL_INTERVAL_MS = 2000;
const MAX_POLL_ATTEMPTS = 90; // ~3 dəqiqə

// Backend GenerationStage → hansı sətir hazırda icra olunur (0, 1, 2)
const STAGE_INDEX = {
  ANALYZING_INTERESTS: 0,
  SELECTING_PLACES: 1,
  BUILDING_ITINERARY: 2,
  DONE: 3,
};

const STEPS = [
  { done: "Maraq dairən təhlil olundu", active: "Maraq dairən təhlil olunur..." },
  { done: "Uyğun məkanlar seçildi", active: "Uyğun məkanlar seçilir..." },
  { done: "Gündəlik marşrut quruldu", active: "Gündəlik marşrut qurulur..." },
];

/** GET /api/plans/{id}/status ilə planın hazır olmasını gözləyir, sonra /travel/:planId-yə keçir. */
function PlanLoading() {
  const { planId } = useParams();
  const navigate = useNavigate();
  const [activeIndex, setActiveIndex] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    let timer = null;
    let attempts = 0;

    const poll = async () => {
      if (cancelled) return;

      try {
        const status = await getPlanStatus(planId);
        if (cancelled) return;

        if (status.status === "READY") {
          navigate(`/travel/${planId}`, { replace: true });
          return;
        }

        if (status.status === "FAILED") {
          setError(status.message || "Plan hazırlanarkən xəta baş verdi.");
          return;
        }

        if (status.stage && STAGE_INDEX[status.stage] !== undefined) {
          setActiveIndex(STAGE_INDEX[status.stage]);
        }

        attempts += 1;
        if (attempts >= MAX_POLL_ATTEMPTS) {
          setError(
            "Plan hazırlanması çox uzun çəkdi. Bir az sonra yenidən yoxla."
          );
          return;
        }

        timer = setTimeout(poll, POLL_INTERVAL_MS);
      } catch (caught) {
        if (!cancelled) setError(apiErrorMessage(caught));
      }
    };

    poll();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, [planId, navigate]);

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-canvas p-4 font-sans">
        <div className="w-full max-w-md rounded-4xl border border-red-100 bg-white p-8 text-center shadow-soft">
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-3xl">
            😕
          </div>

          <h1 className="text-xl font-bold text-ink-900">
            Plan hazırlana bilmədi
          </h1>

          <p role="alert" className="mt-2 text-sm leading-relaxed text-ink-500">
            {error}
          </p>

          <button
            type="button"
            onClick={() => navigate("/")}
            className="mt-7 rounded-full bg-ink-900 px-7 py-3 text-sm font-semibold text-white transition-all duration-200 hover:bg-ink-800 active:scale-95"
          >
            Ana səhifəyə qayıt
          </button>
        </div>
      </div>
    );
  }

  // Ümumi proqres: 3 addım

  // Ümumi proqres: 3 addım
  const progress = ((activeIndex + 1) / STEPS.length) * 100;

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-canvas p-4 font-sans">
      {/* arxa plan işıqları */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -left-24 top-1/4 h-80 w-80 rounded-full bg-brand-200/40 blur-3xl animate-voy-drift"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-20 bottom-1/4 h-72 w-72 rounded-full bg-sun-200/40 blur-3xl animate-voy-float-slow"
      />

      <div className="relative flex w-full max-w-md flex-col items-center text-center">
        {/* pulsuz dəyişən AI orbu */}
        <div className="relative mb-9">
          <span
            aria-hidden="true"
            className="absolute inset-0 rounded-full bg-brand-300/50 animate-voy-pulse-ring"
          />
          <div className="relative flex h-24 w-24 items-center justify-center rounded-full bg-white shadow-lift ring-1 ring-brand-100">
            <img
              src={star}
              alt=""
              className="h-11 w-11 animate-voy-float object-contain"
            />
          </div>
        </div>

        <h1 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">
          AI sənin planını hazırlayır
          <span className="ml-1 inline-block animate-voy-blink">…</span>
        </h1>

        <p className="mt-3 text-sm text-ink-500">
          Adətən bir neçə saniyə çəkir — səhifəni bağlama.
        </p>

        <img
          src={coffe}
          alt=""
          className="my-9 h-20 animate-voy-float-slow object-contain"
        />

        {/* proqres çubuğu */}
        <div className="w-full">
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full bg-gradient-to-r from-brand-500 to-mint-500 transition-all duration-700 ease-out"
              style={{ width: `${progress}%` }}
            />
          </div>

          <p className="mt-2.5 text-[11px] font-semibold uppercase tracking-wider text-ink-400">
            Addım {activeIndex + 1} / {STEPS.length}
          </p>
        </div>


        {/* addım siyahısı */}
        <ul className="mt-9 flex w-full flex-col gap-3 text-left">
          {STEPS.map((step, index) => {
            if (index < activeIndex) {
              return (
                <li
                  key={step.done}
                  className="flex items-center gap-3 text-sm font-medium text-ink-400"
                >
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-mint-500 text-white">
                    <svg
                      className="h-3 w-3"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="3.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                  </span>
                  <span className="line-through decoration-slate-300">
                    {step.done}
                  </span>
                </li>
              );
            }

            if (index === activeIndex) {
              return (
                <li
                  key={step.done}
                  className="flex items-center gap-3 text-sm font-bold text-ink-900 sm:text-base"
                >
                  <span className="relative flex h-5 w-5 shrink-0 items-center justify-center">
                    <span className="absolute h-2.5 w-2.5 rounded-full bg-brand-400 animate-voy-pulse-ring" />
                    <span className="relative h-2.5 w-2.5 rounded-full bg-brand-500" />
                  </span>
                  {step.active}
                </li>
              );
            }

            return (
              <li
                key={step.done}
                className="flex items-center gap-3 text-sm text-ink-300"
              >
                <span className="h-5 w-5 shrink-0 rounded-full border-2 border-slate-200" />
                {step.active}
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

export default PlanLoading;
