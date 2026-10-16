import Reveal from "./Reveal";

/**
 * Bölmə başlığı — bütün ana səhifə bölmələri vahid vizual ritmə malik olsun.
 * @param {{ eyebrow?: {text:string, tone?:'brand'|'mint'|'sun', icon?:string},
 *           title: string, description?: string, align?: 'left'|'center' }} props
 */
function SectionHeading({
  eyebrow,
  title,
  description,
  align = "left",
  className = "",
}) {
  const isCenter = align === "center";
  const tone = eyebrow?.tone ?? "brand";

  const dotClass = {
    brand: "bg-brand-500",
    mint: "bg-mint-500",
    sun: "bg-sun-500",
  }[tone];

  const textClass = {
    brand: "text-brand-600",
    mint: "text-mint-600",
    sun: "text-sun-700",
  }[tone];

  return (
    <div
      className={`${isCenter ? "mx-auto max-w-2xl text-center" : "max-w-2xl"} ${className}`}
    >
      {eyebrow && (
        <Reveal
          className={`mb-3 inline-flex items-center gap-2 rounded-full border px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-[0.12em] ${
            tone === "brand"
              ? "border-brand-100 bg-brand-50"
              : tone === "mint"
                ? "border-mint-200 bg-mint-50"
                : "border-sun-200 bg-sun-50"
          }`}
        >
          <span className="relative flex h-1.5 w-1.5">
            <span
              className={`absolute inline-flex h-full w-full rounded-full ${dotClass} opacity-60 animate-voy-pulse-ring`}
            />
            <span className={`relative inline-flex h-1.5 w-1.5 rounded-full ${dotClass}`} />
          </span>

          <span className={textClass}>
            {eyebrow.icon && <span className="mr-1">{eyebrow.icon}</span>}
            {eyebrow.text}
          </span>
        </Reveal>
      )}

      <Reveal delay={70}>
        <h2 className="text-3xl font-extrabold leading-[1.15] tracking-tight text-ink-900 sm:text-4xl lg:text-[2.65rem]">
          {title}
        </h2>
      </Reveal>

      {description && (
        <Reveal delay={140}>
          <p
            className={`mt-3 text-base leading-relaxed text-ink-500 sm:text-[17px] ${
              isCenter ? "mx-auto max-w-xl" : "max-w-xl"
            }`}
          >
            {description}
          </p>
        </Reveal>
      )}
    </div>
  );
}

export default SectionHeading;
