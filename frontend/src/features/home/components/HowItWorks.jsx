import { useEffect, useRef, useState } from "react";
import comment from "../../../assets/comment.png";
import star from "../../../assets/star.png";
import save from "../../../assets/save.png";
import SectionHeading from "../../../components/common/SectionHeading";

/**
 * "Necə işləyir" — statik 3 kart əvəzinə video təsirli, avtomatik
 * irəliləyən addımlar sahəsi (PO tələbi #4).
 *
 * İstifadəçi heç nə etməsə də addımlar avtomatik dəyişir; kliklə də
 * idarə olunur. Sol tərəfdə "pərdə" dəyişir, sağda addımlar seçilir.
 */
const STEPS = [
  {
    id: 1,
    title: "Suallara cavab ver",
    description:
      "Maraq dairən, kiminlə getdiyin və büdcən haqqında qısa sual-cavab",
    icon: comment,
    accent: "#5B8DEF",
    bgColor: "bg-[#EAF0FE]",
    bullets: [
      { icon: "🖼️", text: "Təbiət, dəniz, tarix" },
      { icon: "👥", text: "Tək / cüt / ailə / dostlar" },
      { icon: "💰", text: "Büdcə aralığı" },
    ],
  },
  {
    id: 2,
    title: "AI planını hazırlayır",
    description: "Süni intellekt sənə uyğun fərdi səyahət planı yaradır",
    icon: star,
    accent: "#F2C230",
    bgColor: "bg-[#FDF0C4]",
    bullets: [
      { icon: "🗺️", text: "Gündəlik marşrut" },
      { icon: "🏨", text: "Otel növü tövsiyəsi" },
      { icon: "🍽️", text: "Yemək seçimləri" },
    ],
  },
  {
    id: 3,
    title: "Planını yadda saxla",
    description: "Bəyəndiyini seçib hesabında saxla, istədiyin vaxt bax",
    icon: save,
    accent: "#3DBE7A",
    bgColor: "bg-[#D2F5E2]",
    bullets: [
      { icon: "❤️", text: "Favoritlərə əlavə et" },
      { icon: "📱", text: "Hər cihazda əlçatan" },
      { icon: "✈️", text: "Hazırlıqla səyahətə çıx" },
    ],
  },
];

const AUTO_MS = 4200;

function HowItWorks() {
  const [active, setActive] = useState(0);
  const timerRef = useRef(null);

  // Avtomatik irəliləyən "video" — hərəkət azaltma olmayan mühitdə
  useEffect(() => {
    const prefersReduced = window.matchMedia?.(
      "(prefers-reduced-motion: reduce)"
    ).matches;
    if (prefersReduced) return undefined;

    timerRef.current = window.setInterval(() => {
      setActive((prev) => (prev + 1) % STEPS.length);
    }, AUTO_MS);

    return () => window.clearInterval(timerRef.current);
  }, []);

  const goTo = (index) => {
    setActive(index);
    // istifadəçi əl ilə keçdikdə taymeri yenidən başlat
    window.clearInterval(timerRef.current);
    timerRef.current = window.setInterval(() => {
      setActive((prev) => (prev + 1) % STEPS.length);
    }, AUTO_MS);
  };

  return (
    <section className="relative overflow-hidden bg-white py-20 lg:py-28">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute left-1/2 top-0 h-[300px] w-[600px] -translate-x-1/2 rounded-full bg-brand-100/40 blur-3xl"
      />

      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <SectionHeading
          align="center"
          eyebrow={{ text: "Necə işləyir", tone: "mint", icon: "⚡" }}
          title="3 addımda səyahət planın"
          description="Sadə suallar, güclü AI və sənin üçün hazırlanmış marşrut."
        />

        <div className="mt-14 grid grid-cols-1 items-center gap-10 lg:grid-cols-2 lg:gap-16">
          {/* ================= SOL: video kimi dəyişən "pərdə" ================= */}
          <div className="relative">
            <div className="relative overflow-hidden rounded-[32px] bg-gradient-to-br from-[#EAF0FE] via-[#FDF0C4] to-[#D2F5E2] p-8 shadow-lift sm:p-10">
              <div
                aria-hidden="true"
                className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-white/40 blur-2xl"
              />
              <div
                aria-hidden="true"
                className="pointer-events-none absolute -bottom-20 -left-10 h-48 w-48 rounded-full bg-brand-200/30 blur-2xl animate-voy-float-slow"
              />

              <div className="relative flex items-center justify-between">
                <span className="rounded-full bg-white/70 px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-[0.14em] text-ink-600 backdrop-blur-sm">
                  Addım {active + 1} / {STEPS.length}
                </span>

                <div className="flex items-center gap-1.5">
                  {STEPS.map((item, index) => (
                    <span
                      key={item.id}
                      className={`h-1.5 rounded-full transition-all duration-500 ${
                        index === active ? "w-6" : "w-1.5 opacity-40"
                      }`}
                      style={{ backgroundColor: item.accent }}
                    />
                  ))}
                </div>
              </div>

              {/* dəyişən məzmun */}
              <div className="relative mt-8 min-h-[230px]">
                {STEPS.map((item, index) => (
                  <div
                    key={item.id}
                    aria-hidden={index !== active}
                    className={`transition-all duration-700 ${
                      index === active
                        ? "relative translate-y-0 opacity-100"
                        : "pointer-events-none absolute inset-0 translate-y-6 opacity-0"
                    }`}
                    style={{ transitionTimingFunction: "var(--ease-out-expo)" }}
                  >
                    <div
                      className={`mb-6 flex h-16 w-16 items-center justify-center rounded-2xl ${item.bgColor} shadow-soft`}
                    >
                      <img src={item.icon} alt="" className="h-8 w-8 object-contain" />
                    </div>

                    <h3 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">
                      {item.title}
                    </h3>

                    <p className="mt-3 max-w-md text-sm leading-relaxed text-ink-600 sm:text-base">
                      {item.description}
                    </p>

                    <ul className="mt-6 space-y-2.5">
                      {item.bullets.map((bullet) => (
                        <li
                          key={bullet.text}
                          className="flex items-center gap-3 text-sm font-medium text-ink-700"
                        >
                          <span
                            className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs"
                            style={{ backgroundColor: item.bgColor }}
                            aria-hidden="true"
                          >
                            {bullet.icon}
                          </span>
                          {bullet.text}
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </div>
          </div>


          {/* ================= SAĞ: addım siyahısı ================= */}
          <div className="flex flex-col gap-4">
            {STEPS.map((item, index) => {
              const isActive = index === active;

              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => goTo(index)}
                  aria-current={isActive ? "step" : undefined}
                  className={`group relative flex items-start gap-5 overflow-hidden rounded-3xl border bg-white p-5 text-left transition-all duration-500 sm:p-6 ${
                    isActive
                      ? "border-transparent shadow-lift"
                      : "border-slate-100 shadow-soft hover:border-slate-200 hover:shadow-card"
                  }`}
                >
                  <span
                    aria-hidden="true"
                    className={`absolute inset-y-0 left-0 w-1 transition-all duration-500 ${
                      isActive ? "opacity-100" : "opacity-0"
                    }`}
                    style={{ backgroundColor: item.accent }}
                  />

                  <span
                    className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl transition-all duration-500 ${
                      isActive ? `${item.bgColor} scale-105` : "bg-slate-100"
                    }`}
                  >
                    <img
                      src={item.icon}
                      alt=""
                      className={`h-6 w-6 object-contain transition-opacity duration-500 ${
                        isActive ? "opacity-100" : "opacity-45 grayscale"
                      }`}
                    />
                  </span>

                  <span className="min-w-0 flex-1">
                    <span className="flex items-center gap-2">
                      <span className="text-[11px] font-bold uppercase tracking-wider text-ink-400">
                        Addım {item.id}
                      </span>
                      {isActive && (
                        <span
                          className="inline-flex h-1.5 w-1.5 rounded-full animate-voy-blink"
                          style={{ backgroundColor: item.accent }}
                        />
                      )}
                    </span>

                    <span
                      className={`mt-1 block text-lg font-bold transition-colors duration-500 ${
                        isActive ? "text-ink-900" : "text-ink-600"
                      }`}
                    >
                      {item.title}
                    </span>

                    <span className="mt-1.5 block text-sm leading-relaxed text-ink-500">
                      {item.description}
                    </span>
                  </span>
                </button>
              );
            })}

            {/* proqres çubuğu */}
            <div className="mt-2 flex items-center gap-3 rounded-2xl bg-slate-50 px-5 py-4">
              <span className="text-[11px] font-bold uppercase tracking-wider text-ink-400">
                Addım {active + 1}
              </span>

              <div className="flex flex-1 gap-1.5">
                {STEPS.map((item, index) => (
                  <div
                    key={item.id}
                    className="h-1 flex-1 overflow-hidden rounded-full bg-slate-200"
                  >
                    <div
                      className="h-full rounded-full"
                      style={{
                        backgroundColor: item.accent,
                        width: index <= active ? "100%" : "0%",
                        opacity: index === active ? 1 : 0.5,
                        transition: `width ${index === active ? `${AUTO_MS}ms` : "400ms"} linear`,
                      }}
                    />
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

export default HowItWorks;
