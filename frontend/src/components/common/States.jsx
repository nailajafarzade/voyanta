/** Yüklənir / boş / xəta vəziyyatləri üçün vahid komponentlər. */

/** Bir neçə kart üçün skelet yükləyici. */
function CardSkeleton({ count = 4, className = "" }) {
  return (
    <div
      className={`grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4 ${className}`}
      role="status"
      aria-label="Yüklənir"
    >
      {Array.from({ length: count }).map((_, index) => (
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
  );
}

/** Boş vəziyyət — heç bir şey tapılmadı. */
function EmptyState({ icon = "🧳", title, description, action, className = "" }) {
  return (
    <div
      className={`flex flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white/70 px-6 py-16 text-center ${className}`}
    >
      <div className="mb-5 flex h-20 w-20 items-center justify-center rounded-full bg-brand-50 text-4xl">
        {icon}
      </div>

      <h3 className="text-lg font-bold text-ink-900">{title}</h3>

      {description && (
        <p className="mt-2 max-w-md text-sm leading-relaxed text-ink-500">
          {description}
        </p>
      )}

      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}

/** Xəta vəziyyəti — yenidən cəhd düyməsi ilə. */
function ErrorState({ message, onRetry, className = "" }) {
  return (
    <div
      role="alert"
      className={`flex flex-col items-center justify-center rounded-3xl border border-red-100 bg-red-50/60 px-6 py-12 text-center ${className}`}
    >
      <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-white text-3xl shadow-soft">
        😕
      </div>

      <h3 className="text-base font-bold text-ink-900">
        Məlumat yüklənmədi
      </h3>

      <p className="mt-1.5 max-w-md text-sm leading-relaxed text-ink-500">
        {message}
      </p>

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-6 inline-flex items-center gap-2 rounded-full bg-ink-900 px-6 py-2.5 text-sm font-semibold text-white transition-all duration-200 hover:bg-ink-800 active:scale-95"
        >
          <span>Yenidən cəhd et</span>
          <span aria-hidden="true">↻</span>
        </button>
      )}
    </div>
  );
}

export { CardSkeleton, EmptyState, ErrorState };
