import OptionGrid from "./OptionGrid";
import StepShell from "./StepShell";

const HOTELS = [
  { id: "3star", title: "3 ★", subtitle: "Qənaətbəxəş" },
  { id: "4star", title: "4 ★", subtitle: "Rahat" },
  { id: "5star", title: "5 ★", subtitle: "Lüks" },
  { id: "boutique", title: "Boutique", subtitle: "Unikal", emoji: "🏡" },
  { id: "villa", title: "Villa", subtitle: "Müstəqil", emoji: "🏖️" },
];

function StepFiveHotelType({ formData, setFormData }) {
  return (
    <StepShell
      title="Hansı otel tipini seçirsən?"
      description="Qalacağın yerin səviyyəsini müəyyən edir."
    >
      <OptionGrid
        ariaLabel="Otel tipi"
        columns="grid-cols-2 sm:grid-cols-3"
        options={HOTELS}
        value={formData.hotelType}
        onSelect={(id) => setFormData({ ...formData, hotelType: id })}
      />
    </StepShell>
  );
}

export default StepFiveHotelType;
