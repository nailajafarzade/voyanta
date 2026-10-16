import OptionGrid from "./OptionGrid";
import StepShell from "./StepShell";

const MEALS = [
  { id: "none", title: "Yemək olmasın", subtitle: "Yalnız qalma", emoji: "🚫" },
  { id: "breakfast", title: "Səhər yeməyi", subtitle: "Səhər daxil", emoji: "🥐" },
  { id: "all_inclusive", title: "Hər şey daxil", subtitle: "Tam paket", emoji: "🍹" },
];

function StepSixMealPreference({ formData, setFormData }) {
  return (
    <StepShell
      title="Yemək üstünlüyün nədir?"
      description="Otel paketinə daxil olan yemək tipini seç."
    >
      <OptionGrid
        ariaLabel="Yemək üstünlüyü"
        options={MEALS}
        value={formData.mealPreference}
        onSelect={(id) => setFormData({ ...formData, mealPreference: id })}
      />
    </StepShell>
  );
}

export default StepSixMealPreference;
