import { useEffect, useState } from "react";
import voyanta from "../../assets/Logo Mark.png";
import { useNavigate } from "react-router-dom";

import StepOneInterests from "./components/StepOneInterests";
import StepTwoCompanions from "./components/StepTwoCompanions";
import StepThreeBudget from "./components/StepThreeBudget";
import StepFourDates from "./components/StepFourDates";
import StepFiveHotelType from "./components/StepFiveHotelType";
import StepSixMealPreference from "./components/StepSixMealPreference";
import StepSevenTripPurpose from "./components/StepSevenTripPurpose";

import { createSession, updateSession } from "../../api/survey";
import { generatePlan } from "../../api/plans";
import { apiErrorMessage } from "../../api/errors";
import { setPendingPlanId } from "./storage";
import { buildSurveyPatch, isStepValid } from "./wizard";

const INITIAL_FORM_DATA = {
  interests: [],
  companion: "",
  peopleCount: 2,

  adults: 2,
  children: 1,

  budgetType: "",
  customBudget: "",
  startDate: "",
  endDate: "",

  hotelType: "",
  mealPreference: "",
  tripPurpose: [],
};

function PlanningPage({ isOpen, onClose }) {
  const TOTAL_STEPS = 7;

  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState(INITIAL_FORM_DATA);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Sorğu açıq olduqa Escape ilə bağlanır və arxa plan sürüşməsinin qarşısı alınır
  useEffect(() => {
    if (!isOpen) return undefined;

    const onKey = (event) => {
      if (event.key === "Escape" && !isSubmitting) {
        setError(null);
        onClose();
      }
    };

    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [isOpen, isSubmitting, onClose]);

  // Bütün hook-lar çağırıldıqdan sonra qısa yol (Rules of Hooks)
  if (!isOpen) return null;

  // Hər step üçün seçim edilib-edilmədiyini yoxlayırıq
  const canGoNext = () => isStepValid(currentStep, formData);

  const updateFormData = (next) => {
    setError(null);
    setFormData(next);
  };

  const handleNext = () => {
    if (!canGoNext()) {
      return;
    }

    if (currentStep < TOTAL_STEPS) {
      setCurrentStep((prev) => prev + 1);
    }
  };

  const handlePrev = () => {
    setError(null);

    if (currentStep > 1) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const handleClose = () => {
    if (isSubmitting) return;
    setError(null);
    onClose();
  };

  /**
   * Cavabları backend-ə göndərib planı başladır:
   * POST /survey/sessions → PATCH /survey/sessions/{id} → POST /plans/generate.
   * Hər dəfə YENİ sessiya açılır, çünki backend eyni sessiya üçün mövcud planı qaytarır.
   * Sonra gözləmə ekranına (/loading/:planId) keçir.
   */
  const handleFinish = async () => {
    if (isSubmitting) return;

    for (let step = 1; step <= TOTAL_STEPS; step += 1) {
      if (!isStepValid(step, formData)) {
        setCurrentStep(step);
        return;
      }
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const session = await createSession();
      await updateSession(session.sessionId, buildSurveyPatch(formData));
      const planId = await generatePlan(session.sessionId);

      // Anonim istifadəçi sonradan daxil olanda bu plan hesaba bağlanacaq
      setPendingPlanId(planId);

      setFormData(INITIAL_FORM_DATA);
      setCurrentStep(1);
      onClose();
      navigate(`/loading/${planId}`);
    } catch (caught) {
      setError(apiErrorMessage(caught));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/60 p-4 backdrop-blur-md">
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Səyahət planlaşdırma"
        className="relative flex max-h-[92vh] w-full max-w-[680px] animate-voy-pop flex-col justify-between overflow-y-auto rounded-4xl border border-slate-100 bg-canvas p-6 shadow-lift sm:min-h-[500px] sm:p-10"
      >

        <div>
          {/* HEADER */}
          <div className="mb-8 flex w-full items-center justify-between">

            <div className="hidden sm:flex sm:items-center sm:gap-2">
              <img
                src={voyanta}
                alt="Voyanta"
                className="w-6 h-6 object-contain"
              />

              <span className="font-bold text-xl tracking-tight text-slate-900">
                voyanta
              </span>
            </div>

            {/* Mobile back */}
            <button
              type="button"
              onClick={handlePrev}
              disabled={currentStep === 1}
              className={`sm:hidden p-1 text-slate-600 hover:text-slate-900 transition-colors ${
                currentStep === 1
                  ? "opacity-0 pointer-events-none"
                  : "opacity-100"
              }`}
              aria-label="Geri"
            >
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18"
                />
              </svg>
            </button>

            {/* Progress */}
            <div className="flex items-center gap-1.5">
              {Array.from({ length: TOTAL_STEPS }).map(
                (_, index) => {
                  const stepNum = index + 1;

                  return (
                    <div
                      key={stepNum}
                      className={`h-1.5 rounded-full transition-all duration-300 ${
                        stepNum <= currentStep
                          ? "w-6 sm:w-7 bg-brand-500"
                          : "w-4 sm:w-5 bg-slate-200"
                      }`}
                    />
                  );
                }
              )}
            </div>

            {/* Close */}
            <button
              type="button"
              onClick={handleClose}
              className="text-slate-400 hover:text-slate-600 transition-colors p-1"
              aria-label="Bağla"
            >
              <svg
                className="w-6 h-6 sm:w-5 sm:h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>

          {/* STEPS */}
          <div className="w-full my-2">

            {currentStep === 1 && (
              <StepOneInterests
                formData={formData}
                setFormData={updateFormData}
              />
            )}

            {currentStep === 2 && (
              <StepTwoCompanions
                formData={formData}
                setFormData={updateFormData}
              />
            )}

            {currentStep === 3 && (
              <StepThreeBudget
                formData={formData}
                setFormData={updateFormData}
              />
            )}

            {currentStep === 4 && (
              <StepFourDates
                formData={formData}
                setFormData={updateFormData}
              />
            )}

            {currentStep === 5 && (
              <StepFiveHotelType
                formData={formData}
                setFormData={updateFormData}
              />
            )}

            {currentStep === 6 && (
              <StepSixMealPreference
                formData={formData}
                setFormData={updateFormData}
              />
            )}

            {currentStep === 7 && (
              <StepSevenTripPurpose
                formData={formData}
                setFormData={updateFormData}
              />
            )}

          </div>
        </div>

        <div>
          {error && (
            <p
              role="alert"
              className="w-full text-xs text-red-600 bg-red-50 border border-red-100 rounded-2xl px-4 py-2.5 mt-6 text-center leading-relaxed"
            >
              {error}
            </p>
          )}

          {/* BUTTONS */}
          <div className="flex items-center justify-center gap-3 mt-8">

            {currentStep > 1 && (
              <button
                type="button"
                onClick={handlePrev}
                disabled={isSubmitting}
                className="hidden sm:block px-7 py-2.5 rounded-full border border-slate-200 bg-white text-slate-700 text-sm font-medium hover:bg-slate-50 transition-colors disabled:opacity-50"
              >
                Geri
              </button>
            )}

            {currentStep < TOTAL_STEPS ? (
              <button
                type="button"
                onClick={handleNext}
                disabled={!canGoNext()}
                className={`w-full sm:w-auto px-8 py-3 sm:py-2.5 rounded-full text-white text-sm font-semibold transition-all shadow-sm flex items-center justify-center gap-1.5 ${
                  canGoNext()
                    ? "bg-brand-500 hover:bg-brand-600"
                    : "bg-slate-300 cursor-not-allowed"
                }`}
              >
                <span>Növbəti</span>
                <span>→</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={handleFinish}
                disabled={!canGoNext() || isSubmitting}
                className={`w-full sm:w-auto px-8 py-3 sm:py-2.5 rounded-full text-white text-sm font-semibold transition-all shadow-sm flex items-center justify-center gap-1.5 ${
                  canGoNext() && !isSubmitting
                    ? "bg-brand-500 hover:bg-brand-600"
                    : "bg-slate-300 cursor-not-allowed"
                }`}
              >
                <span>{isSubmitting ? "Göndərilir..." : "Uyğunlaşmaları göstər"}</span>
                {!isSubmitting && <span>✨</span>}
              </button>
            )}

          </div>
        </div>
      </div>
    </div>
  );
}

export default PlanningPage;
