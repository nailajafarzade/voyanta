import medium from "../../../assets/medium.png";
import comfort from "../../../assets/comfort.png";
import premium from "../../../assets/premium.png";
import OptionGrid from "./OptionGrid";
import StepShell from "./StepShell";

const BUDGETS = [
  { id: "medium", title: "Orta", subtitle: "800 – 1200 AZN", icon: medium },
  { id: "comfort", title: "Rahat", subtitle: "1200 – 2000 AZN", icon: comfort },
  { id: "premium", title: "Premium", subtitle: "2000+ AZN", icon: premium },
];

function StepThreeBudget({ formData, setFormData }) {
  // Dəqiq məbləğ yazılanda seçilmiş səviyyə ləğv olunur (backend-də CUSTOM büdcə)
  const handleCustomBudget = (value) => {
    setFormData({
      ...formData,
      customBudget: value,
      budgetType:
        value.trim() !== ""
          ? "custom"
          : formData.budgetType === "custom"
            ? ""
            : formData.budgetType,
    });
  };

  return (
    <StepShell
      title="Büdcən nə qədərdir?"
      description="Bu səyahət üçün nəzərdə tutduğun ümumi büdcəni seç (nəfər başına)."
    >
      <OptionGrid
        ariaLabel="Büdcə səviyyəsi"
        options={BUDGETS}
        value={formData.budgetType}
        onSelect={(id) =>
          setFormData({ ...formData, budgetType: id, customBudget: "" })
        }
      />

      <div className="mx-auto mt-6 w-full max-w-xl text-left">

        <div className="relative">
          {formData.customBudget && (
            <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-xs font-semibold text-ink-400">
              AZN
            </span>
          )}
        </div>
      </div>
    </StepShell>
  );
}

export default StepThreeBudget;
