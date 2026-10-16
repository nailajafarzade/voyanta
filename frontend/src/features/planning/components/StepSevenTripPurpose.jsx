import OptionGrid from "./OptionGrid";
import StepShell from "./StepShell";

const PURPOSES = [
  { id: "honeymoon", title: "Bal ayı", emoji: "💍" },
  { id: "instagram", title: "Kontent üçün", emoji: "📷" },
  { id: "relaxation", title: "Rahatlıq", emoji: "🧘" },
  { id: "adventure", title: "Macəra", emoji: "🧗" },
];

function StepSevenTripPurpose({ formData, setFormData }) {
  const togglePurpose = (id) => {
    const exists = formData.tripPurpose.includes(id);
    const updated = exists
      ? formData.tripPurpose.filter((item) => item !== id)
      : [...formData.tripPurpose, id];

    setFormData({ ...formData, tripPurpose: updated });
  };

  return (
    <StepShell
      title="Səyahətinin məqsədi nədir?"
      description="Bir və ya bir neçə seçim edə bilərsən."
    >
      <OptionGrid
        ariaLabel="Səyahət məqsədi"
        multiple
        columns="grid-cols-2 sm:grid-cols-4"
        options={PURPOSES}
        value={formData.tripPurpose}
        onSelect={togglePurpose}
      />
    </StepShell>
  );
}

export default StepSevenTripPurpose;
