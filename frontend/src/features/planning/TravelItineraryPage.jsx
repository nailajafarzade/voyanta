import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PlanningPage from "./PlanningPage";
import BudgetSummary from "./travelinfo/BudgetSummary";
import { getPlan, getPlanStatus } from "../../api/plans";
import { apiErrorMessage } from "../../api/errors";
import { useAuth } from "../../context/authContext";
import { useAuthModal } from "../../context/authModalContext";
import DestinationImage from "../../components/common/DestinationImage";
import FavoriteButton from "../../components/common/FavoriteButton";
import { COMPANION_LABELS } from "./labels";
import { claimAnonymousPlan } from "./claimAnonymousPlan";
import { getPendingPlanId } from "./storage";
import {
  dateForDayNumber,
  formatAzn,
  formatDateRange,
  formatDayLabel,
} from "../../utils/format";
import { normaliseImageUrl } from "../../utils/destinations";


/** /travel/:planId — GET /api/plans/{id} ilə real planı göstərir. */
function TravelItineraryPage() {
  const { planId } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { openLogin } = useAuthModal();

  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [plan, setPlan] = useState(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showBudgetInfo, setShowBudgetInfo] = useState(false);

  const cancelled = useRef(false);
  const claimTried = useRef(false);

  const loadPlan = useCallback(async (id) => {
    const loaded = await getPlan(id);
    if (!cancelled.current) {
      setPlan(loaded);
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    cancelled.current = false;
    claimTried.current = false;
    setIsLoading(true);
    setError(null);
    setPlan(null);

    (async () => {
      try {
        const status = await getPlanStatus(planId);
        if (cancelled.current) return;

        if (status.status === "GENERATING") {
          navigate(`/loading/${planId}`, { replace: true });
          return;
        }

        if (status.status === "FAILED") {
          setError(status.message || "Plan yaradıla bilmədi.");
          setIsLoading(false);
          return;
        }

        await loadPlan(planId);
      } catch (caught) {
        if (!cancelled.current) {
          setError(apiErrorMessage(caught));
          setIsLoading(false);
        }
      }
    })();

    return () => {
      cancelled.current = true;
    };
  }, [planId, navigate, loadPlan]);

  useEffect(() => {
    if (authLoading || !isAuthenticated || !plan) return;
    if (claimTried.current || getPendingPlanId() !== planId) return;

    claimTried.current = true;
    claimAnonymousPlan()
        .then(() => loadPlan(planId))
        .catch(() => {});
  }, [authLoading, isAuthenticated, plan, planId, loadPlan]);

  if (isLoading) {
    return (
        <div className="flex min-h-screen items-center justify-center bg-canvas font-sans">
          <div className="flex flex-col items-center gap-4">
            <span className="h-12 w-12 animate-spin rounded-full border-4 border-slate-200 border-t-brand-500" />
            <p className="text-sm text-ink-500">Plan yüklənir…</p>
          </div>
        </div>
    );
  }

  if (error || !plan) {
    return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-canvas px-4 font-sans">
          <div className="w-full max-w-md rounded-4xl border border-slate-100 bg-white p-8 text-center shadow-soft">
            <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-3xl">
              🧭
            </div>

            <h2 className="text-xl font-extrabold tracking-tight text-ink-900">
              Plan yüklənə bilmədi
            </h2>

            <p role="alert" className="mt-2 text-sm leading-relaxed text-ink-500">
              {error ?? "Plan tapılmadı."}
            </p>

            <Link
                to="/"
                className="mt-7 inline-block rounded-full bg-brand-500 px-7 py-3 text-sm font-semibold text-white shadow-glow transition-all duration-300 hover:bg-brand-600 active:scale-95"
            >
              Ana səhifə
            </Link>
          </div>
        </div>
    );
  }

  const days = [...(plan.days ?? [])].sort((a, b) => a.dayNumber - b.dayNumber);
  const hasLockedDays = days.some((day) => day.locked);
  const currentTitle = plan.destination ?? "Səyahət planın";
  // AI planının istiqaməti əvvəlcədən təyin edilməmiş ola bilər (Greenland, Svalbard, ...),
// ona görə şəkli backend `plan.imageUrl` ilə göndərir — frontend heç bir lokal
// şəkil saxlamır və istiqamətə görə şəkil seçmir.
  const currentImage = normaliseImageUrl(plan.imageUrl);
  const dateRange = formatDateRange(plan.startDate, plan.endDate);
  const companionLabel = plan.companion ? COMPANION_LABELS[plan.companion] : null;
  const total = plan.budgetSummary?.total;

  let itemCounter = 0;

  return (
      <div className="min-h-screen bg-canvas px-4 py-8 font-sans sm:px-6 lg:px-8">
        <div className="mx-auto flex max-w-6xl flex-col gap-6">
          {/* ============ HERO ============ */}
          <div className="relative h-[280px] w-full overflow-hidden rounded-4xl bg-gradient-to-tr from-slate-700 via-slate-500 to-slate-400 shadow-lift sm:h-[320px]">
            {currentImage && (
                <DestinationImage
                    src={currentImage}
                    alt={currentTitle}
                    className="absolute inset-0 h-full w-full"
                    eager
                />
            )}

            {/* Wishlist button — həmişə görünür; istiqamət tapılmazsa sıradan çıxmış olur */}
            <FavoriteButton
                place={{
                  id: plan.destinationId,
                  title: plan.destination,
                  subtitle: "",
                  imageUrl: plan.imageUrl,
                  images: plan.images,
                }}
                size="md"
                variant="glass"
                className="absolute top-3.5 right-3.5 z-30"
            />

            <div className="absolute inset-0 flex flex-col justify-end bg-gradient-to-t from-black/85 via-black/40 to-transparent p-6 sm:p-8">
              <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
                <div className="flex flex-col gap-2.5">
                <span className="inline-flex w-fit items-center gap-1.5 rounded-full border border-white/15 bg-white/10 px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-white backdrop-blur-md">
                  <span aria-hidden="true">✈️</span>
                  Səyahət planın
                </span>

                  <h1 className="text-2xl font-extrabold tracking-tight text-white drop-shadow-sm sm:text-3xl lg:text-4xl">
                    {currentTitle}
                  </h1>

                  <div className="flex flex-wrap items-center gap-2.5 text-xs font-medium text-white/90 sm:text-sm">
                    {dateRange && (
                        <span className="flex items-center gap-1.5 rounded-full border border-white/10 bg-black/30 px-3 py-1.5 backdrop-blur-sm">
                      <span aria-hidden="true">📅</span>
                          {dateRange}
                    </span>
                    )}

                    {companionLabel && (
                        <span className="flex items-center gap-1.5 rounded-full border border-white/10 bg-black/30 px-3 py-1.5 backdrop-blur-sm">
                      <span aria-hidden="true">👨‍👩‍👧</span>
                          {companionLabel}
                    </span>
                    )}

                    {total != null && (
                        <div className="relative">
                          <span className="flex items-center gap-1.5 rounded-full border border-white/10 bg-black/30 px-3 py-1.5 backdrop-blur-sm">
                            <span aria-hidden="true">💰</span>
                            Təxmini {formatAzn(total)}

                            {/* Yalnız bu wrapper hover-i dinləyir (i iconu) */}
                            <span
                                className="relative ml-0.5 flex shrink-0 items-center"
                                onMouseEnter={() => setShowBudgetInfo(true)}
                                onMouseLeave={() => setShowBudgetInfo(false)}
                            >
                              <button
                                  type="button"
                                  onClick={() => setShowBudgetInfo((prev) => !prev)}
                                  onFocus={() => setShowBudgetInfo(true)}
                                  onBlur={() => setShowBudgetInfo(false)}
                                  className="flex h-4 w-4 shrink-0 items-center justify-center rounded-full border border-white/70 text-[10px] font-bold leading-none text-white transition-colors hover:bg-white/20 focus:bg-white/20 focus:outline-none"
                                  aria-label="Büdcə haqqında məlumat"
                                  aria-describedby={showBudgetInfo ? "budget-info-tooltip" : undefined}
                              >
                                i
                              </button>

                              {showBudgetInfo && (
                                  <div
                                      id="budget-info-tooltip"
                                      role="tooltip"
                                      className="pointer-events-none absolute left-full top-1/2 z-50 ml-3 w-64 -translate-y-1/2 rounded-xl border border-slate-100 bg-white p-3 text-left text-xs font-normal leading-relaxed text-ink-600 shadow-card"
                                  >
                                    <span className="absolute -left-1 top-1/2 h-2 w-2 -translate-y-1/2 rotate-45 border-b border-l border-slate-100 bg-white" />
                                    <span className="font-semibold text-ink-900">
                                      Büdcə təxminidir.
                                    </span>{" "}
                                    Seçdiyiniz maraqlar, fəaliyyətlər və səyahət
                                    seçimlərinə görə ümumi xərc dəyişə bilər.
                                  </div>
                              )}
                            </span>
                          </span>
                        </div>
                    )}
                  </div>
                </div>

                <button
                    type="button"
                    onClick={() => setIsWizardOpen(true)}
                    className="shrink-0 self-start whitespace-nowrap rounded-full bg-white px-5 py-2.5 text-xs font-bold text-ink-900 shadow-soft transition-all duration-200 hover:bg-brand-50 active:scale-95 sm:self-auto sm:text-sm"
                >
                  Yenidən planla
                </button>
              </div>
            </div>
          </div>

          <PlanningPage
              isOpen={isWizardOpen}
              onClose={() => setIsWizardOpen(false)}
          />

          {hasLockedDays && !isAuthenticated && (
              <div className="flex flex-col items-start justify-between gap-4 rounded-3xl border border-brand-100 bg-brand-50/70 px-6 py-5 sm:flex-row sm:items-center">
                <div className="flex items-start gap-3">
              <span className="text-xl" aria-hidden="true">
                🔓
              </span>
                  <p className="text-sm leading-relaxed text-ink-700">
                    İlk 2 gün pulsuz göstərilir. Planın qalan günlərini görmək
                    üçün qeydiyyatdan keç.
                  </p>
                </div>

                <button
                    type="button"
                    onClick={openLogin}
                    className="shrink-0 self-start whitespace-nowrap rounded-full bg-brand-500 px-6 py-2.5 text-sm font-semibold text-white shadow-card transition-all duration-200 hover:bg-brand-600 active:scale-95 sm:self-auto"
                >
                  Google ilə davam et
                </button>
              </div>
          )}

          <div className="flex flex-col lg:flex-row items-start gap-6">
            <div className="w-full lg:flex-1 flex flex-col gap-5">
              {days.length === 0 && (
                  <div className="rounded-3xl border border-dashed border-slate-200 bg-white/70 px-6 py-12 text-center">
                    <div className="mb-3 text-3xl" aria-hidden="true">
                      🗓️
                    </div>
                    <p className="text-sm text-ink-500">
                      Bu plan üçün gün məlumatı tapılmadı.
                    </p>
                  </div>
              )}

              {days.map((day) => {
                const dayLabel = formatDayLabel(dateForDayNumber(plan.startDate, day.dayNumber));

                return (
                    <div
                        key={day.dayNumber}
                        className="flex flex-col gap-5 rounded-3xl border border-slate-100 bg-white p-6 shadow-soft transition-shadow duration-300 hover:shadow-card"
                    >
                      <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                        <h2 className="flex items-center gap-2.5 text-lg font-extrabold tracking-tight text-ink-900">
                      <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-brand-50 text-sm font-extrabold text-brand-600">
                        {day.dayNumber}
                      </span>
                          {day.dayNumber}-ci gün
                        </h2>
                        {dayLabel && (
                            <span className="text-xs text-slate-400 font-medium">
                        {dayLabel}
                      </span>
                        )}
                      </div>

                      {day.locked ? (
                          <div className="relative">
                            <div
                                className="flex flex-col gap-4 blur-sm select-none pointer-events-none"
                                aria-hidden="true"
                            >
                              {[0, 1, 2].map((placeholder) => (
                                  <div key={placeholder} className="flex items-start gap-3">
                                    <span className="w-6 h-6 rounded-full bg-slate-100 shrink-0" />
                                    <span className="w-10 h-3 rounded bg-slate-100 shrink-0 mt-1.5" />
                                    <div className="flex flex-col gap-1.5 flex-1">
                                      <span className="h-3 w-2/3 rounded bg-slate-200" />
                                      <span className="h-2.5 w-1/2 rounded bg-slate-100" />
                                    </div>
                                  </div>
                              ))}
                            </div>

                            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-center">
                        <span className="text-2xl" aria-hidden="true">
                          🔒
                        </span>

                              <p className="text-sm font-bold text-ink-900">
                                Bu gün kilidlidir
                              </p>

                              {!isAuthenticated && (
                                  <button
                                      type="button"
                                      onClick={openLogin}
                                      className="rounded-full bg-brand-500 px-6 py-2.5 text-xs font-semibold text-white shadow-card transition-all duration-200 hover:bg-brand-600 active:scale-95"
                                  >
                                    Kilidi açmaq üçün daxil ol
                                  </button>
                              )}
                            </div>
                          </div>
                      ) : (
                          <div className="flex flex-col gap-4">
                            {(day.items ?? []).length === 0 && (
                                <p className="text-sm text-slate-400">Bu gün üçün məlumat yoxdur.</p>
                            )}

                            {(day.items ?? []).map((item, index) => {
                              itemCounter += 1;

                              return (
                                  <div
                                      key={`${day.dayNumber}-${index}`}
                                      className="group flex items-start justify-between gap-3 rounded-2xl p-2 text-sm transition-colors duration-200 hover:bg-slate-50"
                                  >
                                    <div className="flex items-start gap-3">
                              <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-brand-500 text-[10px] font-bold text-white">
                                {itemCounter}
                              </span>

                                      <span className="mt-1 w-10 shrink-0 text-[11px] font-semibold tabular-nums text-ink-400">
                                {item.time ?? ""}
                              </span>

                                      <div className="flex flex-col">
                                <span className="font-semibold text-ink-900">
                                  {item.title}
                                </span>

                                        {item.description && (
                                            <span className="mt-0.5 text-xs leading-relaxed text-ink-400">
                                    {item.description}
                                  </span>
                                        )}
                                      </div>
                                    </div>

                                    {item.category && (
                                        <span className="shrink-0 rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-ink-500">
                                {item.category}
                              </span>
                                    )}
                                  </div>
                              );
                            })}
                          </div>
                      )}
                    </div>
                );
              })}
            </div>

            <BudgetSummary summary={plan.budgetSummary} />
          </div>
        </div>
      </div>
  );
}

export default TravelItineraryPage;