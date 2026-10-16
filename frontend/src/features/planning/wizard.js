/**
 * 7 addımlı sorğu pəncərəsinin (PlanningPage) məlumatını backend müqaviləsinə çevirir.
 * Dizaynın id-ləri (nature, 3star, ...) dəyişmədi — çevirmə yalnız burada olur.
 */

const INTEREST_MAP = {
  nature: "NATURE",
  sea: "SEA",
  culture: "HISTORY_CULTURE",
};

const COMPANION_MAP = {
  solo: "SOLO",
  couple: "COUPLE",
  friends: "FRIENDS",
  family: "FAMILY",
};

const BUDGET_TIER_MAP = {
  medium: "MID_RANGE",
  comfort: "COMFORT",
  premium: "PREMIUM",
};

const HOTEL_MAP = {
  "3star": "THREE_STAR",
  "4star": "FOUR_STAR",
  "5star": "FIVE_STAR",
  boutique: "BOUTIQUE",
  villa: "VILLA",
};

const MEAL_MAP = {
  none: "NO_MEALS",
  breakfast: "BREAKFAST_INCLUDED",
  all_inclusive: "ALL_INCLUSIVE",
};

const PURPOSE_MAP = {
  honeymoon: "HONEYMOON",
  instagram: "INSTAGRAM_CONTENT",
  relaxation: "RELAXATION",
  adventure: "ADVENTURE",
};

/** "650" → 650; boş və ya yanlış dəyər → null */
export function parseCustomBudget(value) {
  const text = String(value ?? "").trim();
  if (!/^\d+$/.test(text)) return null;
  const parsed = Number.parseInt(text, 10);
  return parsed > 0 ? parsed : null;
}

/** Verilən addımın cavabı doludursa true (PlanningPage-də "Növbəti" düyməsi üçün). */
export function isStepValid(step, formData) {
  switch (step) {
    case 1:
      return formData.interests.length > 0;

    case 2:
      return formData.companion !== "";

    case 3:
      if (formData.budgetType === "custom") {
        return parseCustomBudget(formData.customBudget) !== null;
      }
      return formData.budgetType !== "";

    case 4:
      return (
        formData.startDate !== "" &&
        formData.endDate !== "" &&
        formData.endDate >= formData.startDate
      );

    case 5:
      return formData.hotelType !== "";

    case 6:
      return formData.mealPreference !== "";

    case 7:
      return formData.tripPurpose.length > 0;

    default:
      return true;
  }
}

/** PATCH /api/survey/sessions/{id} üçün body — bütün addımlar birdəfəlik göndərilir. */
export function buildSurveyPatch(formData) {
  const patch = {
    interests: formData.interests.map((id) => INTEREST_MAP[id]).filter(Boolean),
    companion: COMPANION_MAP[formData.companion],
    dates: {
      startDate: formData.startDate,
      endDate: formData.endDate,
      approximateDuration: null,
    },
    hotelType: HOTEL_MAP[formData.hotelType],
    mealPreference: MEAL_MAP[formData.mealPreference],
    tripPurpose: formData.tripPurpose.map((id) => PURPOSE_MAP[id]).filter(Boolean),
  };

  // Backend yalnız ailə tərkibini (böyük/uşaq) qəbul edir
  if (formData.companion === "family") {
    patch.familyDetails = { adults: formData.adults, children: formData.children };
  }

  if (formData.budgetType === "custom") {
    patch.budget = { tier: "CUSTOM", exactAmount: parseCustomBudget(formData.customBudget) };
  } else {
    patch.budget = { tier: BUDGET_TIER_MAP[formData.budgetType], exactAmount: null };
  }

  return patch;
}
