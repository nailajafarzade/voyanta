import { useEffect, useRef, useState } from "react";

/**
 * Səhifə aşağı sürüşdükcə elementi " görünür " vəziyyətə gətirir.
 * `prefers-reduced-motion` aktiv olanda heç nə animasiya olunmur.
 *
 * @param {{ children: React.ReactNode, className?: string, delay?: number,
 *           as?: keyof JSX.IntrinsicElements, variant?: 'up'|'left'|'right'|'zoom' }} props
 */
function Reveal({
  children,
  className = "",
  delay = 0,
  as: Tag = "div",
  variant = "up",
  ...rest
}) {
  const ref = useRef(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const node = ref.current;
    if (!node) return undefined;

    // Observer dəstəklənmirsə ya da hərəkət azaltma açıqdursa — dərhal göstər
    if (typeof IntersectionObserver === "undefined") {
      setIsVisible(true);
      return undefined;
    }

    const prefersReduced = window.matchMedia?.(
      "(prefers-reduced-motion: reduce)"
    ).matches;
    if (prefersReduced) {
      setIsVisible(true);
      return undefined;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setIsVisible(true);
            observer.disconnect();
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -60px 0px" }
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  const variantClass =
    variant === "left"
      ? "voy-reveal-left"
      : variant === "right"
        ? "voy-reveal-right"
        : variant === "zoom"
          ? "voy-reveal-zoom"
          : "";

  const classes = [
    "voy-reveal",
    variantClass,
    isVisible ? "voy-reveal-in" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <Tag
      ref={ref}
      className={classes}
      style={delay ? { "--voy-reveal-delay": `${delay}ms` } : undefined}
      {...rest}
    >
      {children}
    </Tag>
  );
}

export default Reveal;
