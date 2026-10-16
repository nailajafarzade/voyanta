import { formatAzn } from "../../../utils/format";

// Backend BudgetSummary: accommodation, food, transport, activities, total
const ROWS = [
  {
    key: "accommodation",
    aliases: ["hotel", "lodging"],
    label: "Yaşayış (otel)",
    icon: "🏨",
    color: "#5B8DEF",
  },
  {
    key: "food",
    aliases: ["meals", "dining"],
    label: "Yemək",
    icon: "🍽️",
    color: "#EF4444",
  },
  {
    key: "transport",
    aliases: ["transportation"],
    label: "Nəqliyyat",
    icon: "🚗",
    color: "#F59E0B",
  },
  {
    key: "activities",
    aliases: ["activity", "entertainment"],
    label: "Fəaliyyətlər",
    icon: "🎟️",
    color: "#3DBE7A",
  },
];

// İngiliscə kateqoriya adını Azərbaycan dilinə çevirir (başqa komponentlərdə də istifadə edə bilərsən)
const CATEGORY_LABELS = {
  accommodation: "Yaşayış (otel)",
  hotel: "Yaşayış (otel)",
  lodging: "Yaşayış (otel)",
  food: "Yemək",
  meals: "Yemək",
  dining: "Yemək",
  transport: "Nəqliyyat",
  transportation: "Nəqliyyat",
  activities: "Fəaliyyətlər",
  activity: "Fəaliyyətlər",
  entertainment: "Fəaliyyətlər",
  total: "Cəmi",
};

export function translateCategory(value) {
  if (!value) return "";
  return CATEGORY_LABELS[String(value).trim().toLowerCase()] ?? value;
}

function readValue(summary, row) {
  const keys = [row.key, ...row.aliases];
  for (const key of keys) {
    const v = summary?.[key];
    if (typeof v === "number") return v;
  }
  return 0;
}

function BudgetSummary({ summary }) {
  const rows = ROWS.map((row) => ({ ...row, value: readValue(summary, row) }));
  const partsTotal = rows.reduce((sum, row) => sum + row.value, 0);
  const total = summary?.total ?? partsTotal;
  const base = partsTotal > 0 ? partsTotal : 1;

  const percent = (value) => Math.round((value / base) * 100);

  return (
      <div className="sticky top-24 flex w-full flex-col gap-5 rounded-4xl border border-slate-100 bg-white p-6 shadow-soft lg:w-80">
        <div className="flex items-center gap-2.5">
          <h3 className="text-base font-extrabold tracking-tight text-ink-900">
            Büdcə xülasəsi
          </h3>

          <span className="ml-auto rounded-full bg-brand-50 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-brand-600">
          Təxmini
        </span>
        </div>

        {/* ümumi məbləğ + paylanma */}
        <div className="flex flex-col gap-3">
          <div>
            <p className="text-2xl font-extrabold tracking-tight text-ink-900">
              {formatAzn(total)}
            </p>
            <p className="mt-0.5 text-xs text-ink-400">təxmini ümumi xərc</p>
          </div>

          <div
              className="flex h-2.5 w-full overflow-hidden rounded-full bg-slate-100"
              role="img"
              aria-label="Büdcənin paylanması"
          >
            {rows.map((row) => (
                <div
                    key={row.key}
                    className="h-full transition-all duration-700"
                    style={{
                      width: `${percent(row.value)}%`,
                      backgroundColor: row.color,
                    }}
                    title={`${row.label}: ${percent(row.value)}%`}
                />
            ))}
          </div>
        </div>

        {/* sətirlər */}
        <ul className="flex flex-col gap-3.5 border-t border-slate-100 pt-5">
          {rows.map((row) => (
              <li key={row.key} className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between text-xs">
              <span className="flex items-center gap-2 font-semibold text-ink-700">
                <span aria-hidden="true" className="text-sm">
                  {row.icon}
                </span>
                {row.label}
              </span>

                  <span className="font-bold text-ink-900">
                {formatAzn(row.value)}
              </span>
                </div>

                <div className="flex items-center gap-2.5">
                  <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-100">
                    <div
                        className="h-full rounded-full transition-all duration-700"
                        style={{
                          width: `${percent(row.value)}%`,
                          backgroundColor: row.color,
                        }}
                    />
                  </div>

                  <span className="w-8 shrink-0 text-right text-[10px] font-bold text-ink-400">
                {percent(row.value)}%
              </span>
                </div>
              </li>
          ))}
        </ul>
      </div>
  );
}

export default BudgetSummary;