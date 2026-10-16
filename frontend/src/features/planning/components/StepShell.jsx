import Reveal from "../../../components/common/Reveal";

/**
 * Sorğunun (7 addımlı wizard) vahid başlıq bloku.
 * Bütün addımlar eyni səviyyədə başlaq, görünüşü bərabər olsun.
 */
function StepShell({ title, description, children, className = "" }) {
  return (
    <div className={`flex w-full flex-col text-center ${className}`}>
      <Reveal>
        <h2 className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl">
          {title}
        </h2>
      </Reveal>

      {description && (
        <Reveal delay={60}>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-ink-500">
            {description}
          </p>
        </Reveal>
      )}

      <Reveal delay={120} className="mt-8 w-full">
        {children}
      </Reveal>
    </div>
  );
}

export default StepShell;
