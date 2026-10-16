import StepShell from "./StepShell";

const MAX_NIGHTS = 14;

function StepFourDates({ formData, setFormData }) {
  const getMaxEndDate = () => {
    if (!formData.startDate) return "";

    const date = new Date(formData.startDate);
    date.setDate(date.getDate() + MAX_NIGHTS);

    return date.toISOString().split("T")[0];
  };

  // Neçə gecə səyahət etdiyini hesablayır
  const getNights = () => {
    if (!formData.startDate || !formData.endDate) return null;

    const start = new Date(formData.startDate);
    const end = new Date(formData.endDate);
    const nights = Math.round((end - start) / 86400000);

    return nights > 0 ? nights : null;
  };

  const handleStartDateChange = (e) => {
    setFormData({ ...formData, startDate: e.target.value, endDate: "" });
  };

  const handleEndDateChange = (e) => {
    setFormData({ ...formData, endDate: e.target.value });
  };

  const nights = getNights();

  const fieldClass =
    "w-full rounded-xl border border-slate-200 bg-white px-4 py-3.5 text-sm text-ink-900 transition-all duration-200 focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-500/10";

  return (
    <StepShell
      title="Nə vaxt səyahət etmək istəyirsən?"
      description={`Gediş və dönüş tarixini seç — maksimum ${MAX_NIGHTS} gecə.`}
    >
      <div className="mx-auto grid w-full max-w-xl grid-cols-1 gap-4 text-left sm:grid-cols-2">
        <div>
          <label
            htmlFor="start-date"
            className="mb-1.5 block text-xs font-medium text-ink-500"
          >
            Gediş tarixi
          </label>
          <input
            id="start-date"
            type="date"
            value={formData.startDate}
            onChange={handleStartDateChange}
            className={fieldClass}
          />
        </div>

        <div>
          <label
            htmlFor="end-date"
            className="mb-1.5 block text-xs font-medium text-ink-500"
          >
            Dönüş tarixi
          </label>
          <input
            id="end-date"
            type="date"
            value={formData.endDate}
            onChange={handleEndDateChange}
            min={formData.startDate || undefined}
            max={getMaxEndDate() || undefined}
            disabled={!formData.startDate}
            className={`${fieldClass} disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-ink-400`}
          />
        </div>
      </div>

      {/* gecə sayğacı */}
      <div className="mx-auto mt-6 w-full max-w-xl">
        <div
          className={`flex items-center justify-center gap-2.5 rounded-full border px-4 py-2.5 text-xs font-medium transition-all duration-300 ${
            nights
              ? "border-mint-200 bg-mint-50 text-mint-700"
              : "border-brand-100 bg-brand-50/70 text-brand-600"
          }`}
        >
          <span aria-hidden="true">{nights ? "🌙" : "ℹ️"}</span>

          <span>
            {nights
              ? `${nights} gecə səyahət etmək istəyirsən`
              : `Gediş tarixindən maksimum ${MAX_NIGHTS} gün ərzində dönüş tarixi seçə bilərsən.`}
          </span>
        </div>
      </div>
    </StepShell>
  );
}

export default StepFourDates;
