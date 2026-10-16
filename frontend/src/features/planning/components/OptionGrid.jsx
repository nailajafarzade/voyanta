/**
 * Seçim kartlarından ibarət vahid grid.
 *
 * Addımlar 3, 5, 6, 7 eyni vizual dili paylaşır: seçili kart mavi çərçivə +
 * yumşaq arxa plan + kölgə, seçilməyən kart isə neytral səth.
 *
 * @param {{ options: Array, value: any, onSelect: (id) => void,
 *           multiple?: boolean, columns?: string, ariaLabel?: string }} props
 */
function OptionGrid({
  options,
  value,
  onSelect,
  multiple = false,
  columns = "grid-cols-1 sm:grid-cols-3",
  ariaLabel = "Seçimlər",
}) {
  const isSelected = (id) =>
    multiple
      ? Array.isArray(value) && value.includes(id)
      : value === id;

  return (
    <div
      role="group"
      aria-label={ariaLabel}
      className={`grid ${columns} gap-3`}
    >
      {options.map((option) => {
        const selected = isSelected(option.id);

        return (
          <button
            key={option.id}
            type="button"
            onClick={() => onSelect(option.id)}
            aria-pressed={selected}
            className={`group flex flex-col items-center justify-center rounded-2xl border p-5 text-center transition-all duration-200 active:scale-[0.97] ${
              selected
                ? "border-brand-500 bg-brand-50/60 text-brand-700 shadow-card"
                : "border-slate-200 bg-white text-ink-700 hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-soft"
            }`}
          >
            {option.icon ? (
              <img
                src={option.icon}
                alt=""
                className={`mb-2 h-8 w-8 object-contain transition-transform duration-300 ${
                  selected ? "scale-110" : "grayscale opacity-70 group-hover:grayscale-0 group-hover:opacity-100"
                }`}
              />
            ) : (
              option.emoji && (
                <span
                  aria-hidden="true"
                  className={`mb-2 text-3xl transition-transform duration-300 ${
                    selected ? "scale-110" : "group-hover:scale-105"
                  }`}
                >
                  {option.emoji}
                </span>
              )
            )}

            {option.title && (
              <span className="text-sm font-bold">{option.title}</span>
            )}

            {option.subtitle && (
              <span
                className={`mt-1 text-xs ${
                  selected ? "text-brand-600/70" : "text-ink-400"
                }`}
              >
                {option.subtitle}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}

export default OptionGrid;
