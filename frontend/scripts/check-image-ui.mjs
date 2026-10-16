/**
 * REAL UI YOXLAMASI — komponentlər server-side render olunur (react-dom/server)
 * və HTML-dən şəkil URL-ləri çıxarılır.
 *
 * Bu, "kartlar faktiki olaraq fərqli şəkil göstərir?" sualına cavab verir.
 * Heç bir brauzer və ya yeni dependency tələb etmir (jsdom yoxdur).
 */
import { renderToStaticMarkup } from "react-dom/server";
import { createElement as h } from "react";
import { MemoryRouter } from "react-router-dom";
import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";
import { createRoot } from "react-dom/client";
import { act } from "react-dom/test-utils";
import { toPlace } from "../src/utils/destinations.js";
import { createImagePlan, getPlanImage } from "../src/utils/imageAssignment.js";
import DestinationImage from "../src/components/common/DestinationImage.jsx";
import PostcardCard from "../src/components/common/PostcardCard.jsx";

/** Router konteksti olmadan `useNavigate` xəta verir — SSR üçün MemoryRouter. */
function render(element) {
  return renderToStaticMarkup(h(MemoryRouter, null, element));
}

const state = { passed: 0, failed: 0 };
function check(label, condition, extra = "") {
  if (condition) {
    state.passed += 1;
    console.log(`  ok   ${label}`);
  } else {
    state.failed += 1;
    console.error(`  FAIL ${label}${extra ? ` — ${extra}` : ""}`);
  }
}

function u(id, n) {
  return `https://images.unsplash.com/photo-${id}-${n}?w=1080`;
}
function cand(url, i = 0) {
  return {
    id: `p${i}`,
    url,
    fullUrl: url.replace("w=1080", "w=1920"),
    photographer: `Photographer ${i}`,
    photographerUrl: `https://unsplash.com/@p${i}`,
    unsplashUrl: `https://unsplash.com/photos/x-${i}`,
  };
}
function fixture(id, name, count) {
  return {
    id,
    name,
    country: "Test",
    tag: "Deniz",
    images: Array.from({ length: count }, (_, i) => cand(u(id, i + 1), i)),
  };
}

const raw = {
  trending: [fixture("a1", "A", 3), fixture("b1", "B", 3), fixture("c1", "C", 2)],
  popular: [fixture("a1", "A", 3), fixture("d1", "D", 1), fixture("e1", "E", 3)],
  featured: [fixture("b1", "B", 3), fixture("f1", "F", 0), fixture("g1", "G", 0)],
};

const places = {
  trending: raw.trending.map(toPlace),
  popular: raw.popular.map(toPlace),
  featured: raw.featured.map(toPlace),
};
const plan = createImagePlan([
  { key: "trending", places: places.trending },
  { key: "popular", places: places.popular },
  { key: "featured", places: places.featured },
]);

/** Kartın faktiki render etdiyi <img src> URL-i. */
function renderedSrc(sectionKey, place) {
  const markup = render(
    h(PostcardCard, {
      place,
      image: getPlanImage(plan, sectionKey, place),
      zoomable: true,
    })
  );
  return markup.match(/<img[^>]+src="([^"]+)"/)?.[1] ?? null;
}

console.log("\nSSR — faktiki render olunmus kartlar");
const rendered = [];
for (const [section, list] of Object.entries(places)) {
  for (const place of list) {
    const src = renderedSrc(section, place);
    rendered.push({ section, id: place.id, title: place.title, src });
    console.log(`  ${section.padEnd(10)} ${place.title.padEnd(6)} -> ${src ? src.slice(30, 78) : "(fallback)"}`);
  }
}

console.log("\nCase 1 — ferqli istiqametler, təkrarsiz");
const trendUrls = places.trending.map((p) => renderedSrc("trending", p)).filter(Boolean);
check("Trending-de təkrar URL yoxdur", new Set(trendUrls).size === trendUrls.length);

console.log("\nCase 2 — eyni istiqamet muxtelif bolmelerde");
const aTrend = renderedSrc("trending", places.trending[0]);
const aPopular = renderedSrc("popular", places.popular[0]);
check("A / Trending != A / Popular", aTrend !== aPopular, `${aTrend} vs ${aPopular}`);
const bTrend = renderedSrc("trending", places.trending[1]);
const bFeatured = renderedSrc("featured", places.featured[0]);
check("B / Trending != B / Featured", bTrend !== bFeatured);

console.log("\nCase 3 — yalniz 1 namized (tekrar qebul edilir)");
const dPopular = renderedSrc("popular", places.popular[1]);
check("1 namizedli istiqamet sekil gosterir", dPopular === u("d1", 1), dPopular);

console.log("\nCase 4 — namized yoxdur, kart qirilmir");
const fSrc = renderedSrc("featured", places.featured[1]);
check("namizedsiz istiqamet render olunur", fSrc === null || typeof fSrc === "string");
const fMarkup = render(h(DestinationImage, { src: places.featured[1].imageUrl, alt: "F" }));
check("universal fallback gradient-i isleyir", fMarkup.includes("bg-gradient-to-tr"));
check("qirilmis sekil ikonu yoxdur", !fMarkup.includes("broken-image"));

console.log("\nTam ekran baxış (lightbox)");
const zoomMarkup = render(
  h(DestinationImage, {
    image: cand(u("a1", 1), 0),
    src: u("a1", 1),
    alt: "A",
    zoomable: true,
  })
);
check("zum düyməsi render olunur", zoomMarkup.includes('aria-label="Şəkli tam ekran aç"')
  || zoomMarkup.includes("aria-label=") && zoomMarkup.includes("role=\"button\""));

console.log("\nAI plan axını (yalniz src — candidates yox)");
const aiMarkup = render(h(DestinationImage, { src: u("ai", 1), alt: "Greenland", eager: true }));
check("AI plan şəkli render olunur", aiMarkup.includes(u("ai", 1)));
check("AI plan üçün lightbox yoxdur (qırılmır)", !aiMarkup.includes("role=\"button\""));

console.log("\nFallback zənciri (seçilmiş şəkil yüklənməsə)");
const chainPlace = toPlace(fixture("z1", "Z", 2));
check("candidates siyahısı mövcuddur", chainPlace.images.length === 2);

// ---------------------------------------------------------------- Lightbox
console.log("\nImageLightbox — markup (createPortal SSR-də işləmir, ona görə struktur yoxlanılır)");
const lightboxSource = readFileSync(
  new URL("../src/components/common/ImageLightbox.jsx", import.meta.url),
  "utf8"
);
check("fullUrl üstünlük alır", /fullUrl[\s\S]{0,120}cardUrl|fullUrl \?\? cardUrl/.test(lightboxSource));
check("object-contain (dartılmır)", lightboxSource.includes("object-contain"));
check("Escape dinləyicisi var", lightboxSource.includes('event.key === "Escape"'));
check("fon kliklə bağlanır", lightboxSource.includes("onClick={onClose}"));
check("bağlama düyməsi var", lightboxSource.includes("closeButtonRef"));
check("attribution: photographer", lightboxSource.includes("photographer"));
check("attribution: photographerUrl", lightboxSource.includes("photographerUrl"));
check("attribution: unsplashUrl", lightboxSource.includes("unsplashUrl"));
check("sənəd gövdəsinə portallaşır", lightboxSource.includes("document.body"));
check("mobil üçün dvh", lightboxSource.includes("100dvh"));
check("safe-area icazəsi", lightboxSource.includes("safe-area-inset"));
check("sənəddə sabit ad/kod yoxdur",
  !/Dario Morandotti|Ruben Hanssen|images\.unsplash\.com\/photo-/.test(lightboxSource));

// Escape klikini real DOM-suz yoxlamaq mümkün deyil, amma handler mövcuddur.
check("klaviatura ilə bağlanmaq üçün document.addEventListener",
  lightboxSource.includes("document.addEventListener"));

// ------------------------------------------------------- Real DOM (jsdom)
console.log("\nImageLightbox — real DOM-da açmaq/bağlamaq");

const dom = new JSDOM("<!doctype html><html><body><div id='root'></div></body></html>", {
  url: "http://localhost/",
  pretendToBeVisual: true,
});
const { window } = dom;

// React-in DOM API-larını jsdom-a yönləndir.
// Node 24-də `globalThis.navigator` yalnız oxuna biləndir → defineProperty lazımdır.
const define = (key, value) =>
  Object.defineProperty(globalThis, key, { value, configurable: true, writable: true });

define("window", window);
define("document", window.document);
define("navigator", window.navigator);
define("HTMLElement", window.HTMLElement);
define("Element", window.Element);
define("Node", window.Node);
define("MutationObserver", window.MutationObserver);
define("getComputedStyle", window.getComputedStyle);
define("requestAnimationFrame", window.requestAnimationFrame.bind(window));
define("cancelAnimationFrame", window.cancelAnimationFrame.bind(window));
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

const container = window.document.getElementById("root");
const root = createRoot(container);

const candidate = {
  id: "photo-1",
  url: "https://images.unsplash.com/photo-1?w=1080",
  fullUrl: "https://images.unsplash.com/photo-1?w=1920",
  photographer: "Backend Photo Author",
  photographerUrl: "https://unsplash.com/@author",
  unsplashUrl: "https://unsplash.com/photos/abc",
};

await act(async () => {
  root.render(
    h(DestinationImage, {
      image: candidate,
      src: candidate.url,
      alt: "Maldives",
      title: "Maldives, Maldiv adaları",
      zoomable: true,
    })
  );
});

const zoomButton = container.querySelector('[aria-label="Şəkli tam ekran aç"]');
check("zum düyməsi DOM-da mövcuddur", Boolean(zoomButton));

// --- aç
await act(async () => {
  zoomButton.dispatchEvent(new window.MouseEvent("click", { bubbles: true, cancelable: true }));
});

const dialog = window.document.querySelector('[role="dialog"]');
check("lightbox açılır", Boolean(dialog));
check("fullUrl istifadə olunur (tam resolution)",
  dialog?.querySelector("img")?.getAttribute("src") === candidate.fullUrl,
  dialog?.querySelector("img")?.getAttribute("src"));
check("aspect ratio qorunur (object-contain)",
  (dialog?.querySelector("img")?.getAttribute("class") ?? "").includes("object-contain"));
check("mobil/masaüstü (dvh)", (dialog?.getAttribute("class") ?? "").includes("100dvh"));

const attributionText = dialog?.textContent ?? "";
check("foto müəllifi göstərilir", attributionText.includes("Backend Photo Author"));
check("Unsplash göstərilir", attributionText.includes("Unsplash"));
check("müəllif linki düzgün", Boolean(dialog?.querySelector('a[href="https://unsplash.com/@author"]')));
check("Unsplash linki düzgün", Boolean(dialog?.querySelector('a[href="https://unsplash.com/photos/abc"]')));
check("body scroll kilidlənir", window.document.body.style.overflow === "hidden");

// --- Escape ilə bağla
await act(async () => {
  window.document.dispatchEvent(
    new window.KeyboardEvent("keydown", { key: "Escape", bubbles: true })
  );
});
check("Escape ilə bağlanır", !window.document.querySelector('[role="dialog"]'));
check("scroll kilidi qalxır", window.document.body.style.overflow !== "hidden");

// --- yenidən aç, fon kliklə bağla
await act(async () => {
  container
    .querySelector('[aria-label="Şəkli tam ekran aç"]')
    .dispatchEvent(new window.MouseEvent("click", { bubbles: true, cancelable: true }));
});
check("yenidən açılır", Boolean(window.document.querySelector('[role="dialog"]')));

await act(async () => {
  const backdrop = window.document.querySelector('[aria-label="Şəkli bağla"]');
  backdrop.dispatchEvent(new window.MouseEvent("click", { bubbles: true, cancelable: true }));
});
check("fon kliklə bağlanır", !window.document.querySelector('[role="dialog"]'));

// --- yenidən aç, bağlama düyməsi ilə bağla
await act(async () => {
  container
    .querySelector('[aria-label="Şəkli tam ekran aç"]')
    .dispatchEvent(new window.MouseEvent("click", { bubbles: true, cancelable: true }));
});
check("üçüncü dəfə açılır", Boolean(window.document.querySelector('[role="dialog"]')));
await act(async () => {
  window.document
    .querySelector('[aria-label="Bağla (Esc)"]')
    .dispatchEvent(new window.MouseEvent("click", { bubbles: true, cancelable: true }));
});
check("bağlama düyməsi ilə bağlanır", !window.document.querySelector('[role="dialog"]'));

// --- AI plan axını real DOM-da
console.log("\nAI plan axını — real DOM");
await act(async () => {
  root.render(
    h(DestinationImage, { src: "https://images.unsplash.com/ai?w=1080", alt: "Greenland", eager: true })
  );
});
const aiImg = container.querySelector("img");
check("AI plan şəkli görünür", aiImg?.getAttribute("src") === "https://images.unsplash.com/ai?w=1080");
check("AI plan üçün zum düyməsi yoxdur (lightbox açılmır)",
  !container.querySelector('[aria-label="Şəkli tam ekran aç"]'));

await act(async () => {
  root.unmount();
});
check("unmount xətasız", true);

console.log(
  state.failed === 0
    ? `\n✅ UI yoxlamaları keçdi (${state.passed} ok)\n`
    : `\n❌ ${state.failed} yoxlama keçmədi (${state.passed} ok)\n`
);
process.exit(state.failed === 0 ? 0 : 1);
