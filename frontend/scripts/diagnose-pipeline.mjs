/**
 * DIAGNOSTIKA: API → hook → toPlace → plan → komponent → render.
 * Heç nə dəyişmir, yalnız mövcud vəziyyəti ölçür.
 */
import { renderToStaticMarkup } from "react-dom/server";
import { createElement as h } from "react";
import { MemoryRouter } from "react-router-dom";

import { toPlace } from "../src/utils/destinations.js";
import { createImagePlan, getPlanImage } from "../src/utils/imageAssignment.js";
import { AuthContext } from "../src/context/authContext.js";
import { AuthModalContext } from "../src/context/authModalContext.js";
import { PlannerContext } from "../src/context/plannerContext.js";
import TrendingPlaces from "../src/features/home/components/TrendingPlaces.jsx";
import PopularPlaces from "../src/features/home/components/PopularPlaces.jsx";

/** Bölmə komponentləri bu 3 konteksti tələb edir — test üçün minimal təchizat. */
const ctx = (node) =>
  h(
    AuthContext.Provider,
    { value: { isAuthenticated: false, isLoading: false } },
    h(
      AuthModalContext.Provider,
      { value: { openLogin: () => {}, closeLogin: () => {} } },
      h(PlannerContext.Provider, { value: { openPlanner: () => {} } }, node)
    )
  );

const BASE = "http://localhost:8080/api";
const json = async (p) => (await (await fetch(`${BASE}${p}`)).json()).data;

const trending = await json("/destinations/trending?limit=6");
const popular = await json("/destinations/popular?limit=6");
const featured = await json("/destinations/featured?limit=12");

console.log("=== STEP 1: API ===");
for (const [n, d] of [["trending", trending], ["popular", popular], ["featured", featured]]) {
  console.log(`${n}: ${d.length} -> ${d.map((x) => x.name).join(", ")}`);
}

console.log("\n=== STEP 2: toPlace (images[] saxlanilir?) ===");
const tp = trending.map(toPlace);
const pp = popular.map(toPlace);
const fp = featured.map(toPlace);
for (const p of tp) {
  console.log(
    `  ${p.title.padEnd(26)} raw=${p.images.length} imageUrl=${p.imageUrl ? "yes" : "NULL"} fullUrl=${p.images[0]?.fullUrl ? "yes" : "NO"}`
  );
}
const misir = tp.find((p) => p.title.startsWith("Misir"));
const ismayilli = tp.find((p) => p.title.includes("smay"));
console.log(`  Misir preserved:     ${Boolean(misir)} images=${misir?.images.length}`);
console.log(`  Ismayilli preserved: ${Boolean(ismayilli)} images=${ismayilli?.images.length}`);

console.log("\n=== STEP 3: plan (seed-siz) ===");
const SECTIONS = [
  { key: "hero", places: fp.slice(0, 4) },
  { key: "trending", places: tp },
  { key: "popular", places: pp },
  { key: "seasonal", places: fp },
];
const plan = createImagePlan(SECTIONS);
for (const p of tp) {
  const img = getPlanImage(plan, "trending", p);
  console.log(
    `  ${p.title.padEnd(26)} -> idx=${p.images.findIndex((c) => c.url === img?.url)}`
  );
}

console.log("\n=== STEP 4: RENDER (real component) ===");
const state = (list) => ({
  places: list,
  allPlaces: list,
  isLoading: false,
  error: null,
  authRequired: false,
  reload: () => {},
});
const html = renderToStaticMarkup(
  h(MemoryRouter, null, ctx(h(TrendingPlaces, { state: state(tp), imagePlan: plan })))
);
for (const n of ["Misir", "smay", "Bak", "Bali", "Dubai", "Kapadokya"]) {
  console.log(`  Trending shows "${n}": ${html.includes(n) ? "YES" : "*** MISSING ***"}`);
}
const imgs = [...html.matchAll(/<img[^>]+src="([^"]+)"/g)].map((m) => m[1]);
console.log(`  rendered <img> count: ${imgs.length}, unique: ${new Set(imgs).size}`);

const htmlP = renderToStaticMarkup(
  h(MemoryRouter, null, ctx(h(PopularPlaces, { state: state(pp), imagePlan: plan })))
);
console.log(`  Popular rendered <img>: ${[...htmlP.matchAll(/<img[^>]+src=/g)].length}`);

console.log("\n=== STEP 5: REFRESH (5 page loads) ===");
const sig = () => JSON.stringify(createImagePlan(SECTIONS));
console.log(`  signatures identical across loads: ${sig() === sig()}`);
const p0 = getPlanImage(plan, "trending", tp[0])?.url;
console.log(`  Misir trending url: ...${(p0 ?? "").slice(45, 80)}`);