import solo from "../../../assets/solo.png";
import couple from "../../../assets/couple.png";
import friends from "../../../assets/friends.png";
import family from "../../../assets/family.png";
import OptionGrid from "./OptionGrid";
import StepShell from "./StepShell";

const COMPANIONS = [
  { id: "solo", title: "Tək", subtitle: "1 nəfər", icon: solo },
  { id: "couple", title: "Cütlük", subtitle: "2 nəfər", icon: couple },
  { id: "friends", title: "Dostlar", subtitle: "Qrupla", icon: friends },
  { id: "family", title: "Ailə", subtitle: "Böyüklər + uşaqlar", icon: family },
];

/** "−" / "+" düyməli rəqəm sayğacı. */
function Counter({ label, hint, value, onDecrease, onIncrease, canDecrease }) {
  return (
    <div className="flex items-center justify-between gap-4 py-3.5">
      <div className="flex min-w-0 flex-col">
        <span className="text-sm font-bold text-ink-900">{label}</span>
        {hint && <span className="mt-0.5 text-xs text-ink-400">{hint}</span>}
      </div>

      <div className="flex shrink-0 items-center gap-3 rounded-xl border border-slate-100 bg-white px-3 py-2">
        <button
          type="button"
          onClick={onDecrease}
          disabled={!canDecrease}
          aria-label={`${label} azalt`}
          className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-100 text-base font-bold text-ink-700 transition-all duration-200 hover:bg-slate-200 active:scale-90 disabled:cursor-not-allowed disabled:opacity-40"
        >
          −
        </button>

        <span className="min-w-[24px] text-center text-sm font-extrabold text-ink-900">
          {value}
        </span>

        <button
          type="button"
          onClick={onIncrease}
          aria-label={`${label} artır`}
          className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-500 text-base font-bold text-white transition-all duration-200 hover:bg-brand-600 active:scale-90"
        >
          +
        </button>
      </div>
    </div>
  );
}

function StepTwoCompanions({ formData, setFormData }) {
  const { adults, children, peopleCount } = formData;

  const handleCompanionChange = (id) => {
    setFormData({
      ...formData,
      companion: id,
      peopleCount: id === "solo" ? 1 : id === "couple" ? 2 : formData.peopleCount || 2,
    });
  };

  const update = (patch) => setFormData({ ...formData, ...patch });

  const showPeopleCounter = formData.companion === "friends" || formData.companion === "family";

  return (
    <StepShell
      title="Bu səyahətə kiminlə gedirsən?"
      description="Planı sənə uyğun fərdiləşdirmək üçün seç."
    >
      <OptionGrid
        ariaLabel="Səyahət yoldaşları"
        columns="grid-cols-2 sm:grid-cols-4"
        options={COMPANIONS}
        value={formData.companion}
        onSelect={handleCompanionChange}
      />

      {/* sayğaclar */}
      {showPeopleCounter && (
        <div className="mx-auto mt-6 w-full max-w-xl divide-y divide-slate-100 rounded-2xl border border-brand-100 bg-brand-50/50 px-5 text-left">
          {formData.companion === "family" ? (
            <>
              <Counter
                label="Böyüklər"
                hint="18 yaş və yuxarı"
                value={adults}
                canDecrease={adults > 1}
                onDecrease={() =>
                  update({
                    adults: Math.max(1, adults - 1),
                    peopleCount: Math.max(1, adults - 1) + children,
                  })
                }
                onIncrease={() =>
                  update({ adults: adults + 1, peopleCount: adults + 1 + children })
                }
              />

              <Counter
                label="Uşaqlar"
                hint="18 yaşdan kiçik"
                value={children}
                canDecrease={children > 0}
                onDecrease={() =>
                  update({
                    children: Math.max(0, children - 1),
                    peopleCount: adults + Math.max(0, children - 1),
                  })
                }
                onIncrease={() =>
                  update({ children: children + 1, peopleCount: adults + children + 1 })
                }
              />
            </>
          ) : (
            <Counter
              label="Neçə nəfərsiniz?"
              hint="Səyahət planını buna uyğun hazırlayacağıq."
              value={peopleCount}
              canDecrease={peopleCount > 2}
              onDecrease={() => update({ peopleCount: Math.max(2, peopleCount - 1) })}
              onIncrease={() => update({ peopleCount: peopleCount + 1 })}
            />
          )}
        </div>
      )}

      {/* seçilmiş kompaniya üzrə xülasə */}
      {formData.companion && (
        <p className="mt-5 text-sm font-semibold text-brand-600">
          {formData.companion === "family"
            ? `${adults} böyük, ${children} uşaq`
            : formData.companion === "friends"
              ? `${peopleCount} nəfər`
              : formData.companion === "couple"
                ? "2 nəfər"
                : "1 nəfər"}{" "}
          səyahət edəcəksiniz
        </p>
      )}
    </StepShell>
  );
}

export default StepTwoCompanions;
