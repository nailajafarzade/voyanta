/**
 * REGRESSIYA TESTLERI — Problem 1/2/3 (A–H).
 * Problem 3 üçün əsas test; Problem 1/2 üçün canlı backend pipeline testləri.
 */
import { renderToStaticMarkup } from "react-dom/server";
import { createElement as h } from "react";
import { MemoryRouter } from "react-router-dom";

import { toPlace } from "../src/utils/destinations.js";
import { toImageCandidates } from "../src/utils/imageCandidates.js";
import { createImagePlan, getPlanImage, PAGE_LOAD_SEED } from "../src/utils/imageAssignment.js";
import { AuthContext } from "../src/context/authContext.js";
import { AuthModalContext } from "../src/context/authModalContext.js";
import { PlannerContext } from "../src/context/plannerContext.js";
import TrendingPlaces from "../src/features/home/components/TrendingPlaces.jsx";
import PopularPlaces from "../src/features/home/components/PopularPlaces.jsx";

const BASE = process.env.VOYANTA_API ?? "http://localhost:8080/api";
let pass = 0;
let fail = 0;
function check(label, ok, extra = "") {
  if (ok) { pass += 1; console.log(`  ok   ${label}`); }
  else { fail += 1; console.error(`  FAIL ${label}${extra ? ` -- ${extra}` : ""}`); }
}

const ctx = (node) =>
  h(AuthContext.Provider, { value: { isAuthenticated: false, isLoading: false } },
    h(AuthModalContext.Provider, { value: { openLogin() {}, closeLogin() {} } },
      h(PlannerContext.Provider, { value: { openPlanner() {} } }, node)));

const state = (list) => ({
  places: list, allPlaces: list, isLoading: false,
  error: null, authRequired: false, reload() {},
});

const u = (id, n) => `https://images.unsplash.com/photo-${id}-${n}?w=1080`;
const cand = (id, n, i) => ({
  id: `${id}-${i}`, url: u(id, n),
  fullUrl: u(id, n).replace("w=1080", "w=1920"),
  photographer: `P${i}`, photographerUrl: `https://unsplash.com/@p${i}`,
  unsplashUrl: `https://unsplash.com/photos/${id}-${i}`,
});
const fx = (id, name, count) => ({
  id, name, country: "X", tag: "Deniz",
  images: Array.from({ length: count }, (_, i) => cand(id, i + 1, i)),
});

const MISIR = fx("misir", "Misir", 3);
const ISMAYILLI = fx("ismayilli", "Ismayilli", 2);
const ONE_ONLY = fx("baki", "Baki", 1);
const NO_IMAGES = { id: "none", name: "None", country: "X", tag: "T", imageUrl: u("none", 1), images: [] };

const SEC = [
  { key: "t", places: [toPlace(MISIR), toPlace(ISMAYILLI)] },
  { key: "p", places: [toPlace(MISIR), toPlace(ISMAYILLI)] },
];

console.log("\n== TEST B: toPlace() images[] siyahisini tam qoruyur ==");
check("images[] saxlanir", toPlace(MISIR).images.length === 3);
check("fullUrl saxlanir", Boolean(toPlace(MISIR).images[0].fullUrl));
check("namized sırası pozulmur",
  toPlace(MISIR).images[0].url === u("misir", 1) && toPlace(MISIR).images[2].url === u("misir", 3));
check("photographer saxlanir", toPlace(MISIR).images[0].photographer === "P0");

console.log("\n== TEST H: images[] yoxdursa imageUrl-a duser ==");
check("images[] bos -> imageUrl namized olur",
  toImageCandidates(NO_IMAGES).length === 1 && toPlace(NO_IMAGES).imageUrl === u("none", 1));
check("hec nə yoxdursa siyahi bos", toImageCandidates({ images: [], imageUrl: "" }).length === 0);
console.log("\n== TEST C: her zaman images[0] secilmir ==");
const idxFor = (seed) => {
  const plan = createImagePlan([{ key: "t", places: [toPlace(MISIR)] }], seed);
  return toPlace(MISIR).images.findIndex((c) => c.url === getPlanImage(plan, "t", toPlace(MISIR)).url);
};
const seen = new Set(Array.from({ length: 12 }, (_, i) => idxFor(i * 7)));
check("ferqli indeksler gorunur", seen.size > 1, `indices=${[...seen]}`);

console.log("\n== TEST E: ferqli seed -> rotasiya ==");
const rot = new Set(Array.from({ length: 10 }, (_, i) => {
  const plan = createImagePlan([{ key: "t", places: [toPlace(MISIR)] }], i);
  return getPlanImage(plan, "t", toPlace(MISIR)).url;
}));
check("10 muxtelif seed -> ferqli sekil", rot.size > 1, `unique=${rot.size}`);

console.log("\n== TEST F: eyni istiqamet ferqli bolmelerde ferqli sekil ==");
const planF = createImagePlan(SEC, 0);
const tM = getPlanImage(planF, "t", toPlace(MISIR));
const pM = getPlanImage(planF, "p", toPlace(MISIR));
check("Misir Trending != Popular (3 namized)", tM.url !== pM.url);
const tI = getPlanImage(planF, "t", toPlace(ISMAYILLI));
const pI = getPlanImage(planF, "p", toPlace(ISMAYILLI));
check("Ismayilli Trending != Popular (2 namized)", tI.url !== pI.url);
check("namized hemise oz destine aidir", toPlace(MISIR).images.some((c) => c.url === tM.url));

console.log("\n== TEST A: Misir/Ismayilli Trending-e catir ==");
let trending = [];
let popular = [];
try {
  trending = (await (await fetch(`${BASE}/destinations/trending?limit=6`)).json()).data;
  popular = (await (await fetch(`${BASE}/destinations/popular?limit=6`)).json()).data;
  console.log(`  backend: ${trending.map((d) => d.name).join(", ")}`);
} catch { console.log("  backend yoxdur - lokal fixture"); }

const tl = trending.length ? trending.map(toPlace) : [MISIR, ISMAYILLI, ONE_ONLY].map(toPlace);
const pl = popular.length ? popular.map(toPlace) : [MISIR, ISMAYILLI].map(toPlace);
const realPlan = createImagePlan([
  { key: "hero", places: tl.slice(0, 4) },
  { key: "trending", places: tl },
  { key: "popular", places: pl },
]);
const htmlT = renderToStaticMarkup(
  h(MemoryRouter, null, ctx(h(TrendingPlaces, { state: state(tl), imagePlan: realPlan }))));
check("Misir Trending-de gorunur", htmlT.includes("Misir"));
check("Ismayilli Trending-de gorunur", htmlT.includes("smay"));
const rendered = [...htmlT.matchAll(/<img[^>]+src="([^"]+)"/g)].map((m) => m[1]);
check("butun trending istiqametleri render olunur",
  rendered.length >= tl.length, `rendered=${rendered.length} of ${tl.length}`);
check("render olunan sekiller unikal", new Set(rendered).size === rendered.length);

const htmlP = renderToStaticMarkup(
  h(MemoryRouter, null, ctx(h(PopularPlaces, { state: state(pl), imagePlan: realPlan }))));
const pUrls = [...htmlP.matchAll(/<img[^>]+src="([^"]+)"/g)].map((m) => m[1]);
check("Popular butun istiqametleri gosterir", pUrls.length === pl.length,
  `rendered=${pUrls.length} of ${pl.length}`);
check("Popular sekilleri unikal", new Set(pUrls).size === pUrls.length);


console.log("\n== TEST G: yalniz 1 sekil ==");
const onePlan = createImagePlan([{ key: "t", places: [toPlace(ONE_ONLY)] }], 0);
check("tek namized secilir", getPlanImage(onePlan, "t", toPlace(ONE_ONLY)).url === u("baki", 1));
check("render olunur", renderToStaticMarkup(
  h(MemoryRouter, null, ctx(h(TrendingPlaces, { state: state([toPlace(ONE_ONLY)]), imagePlan: onePlan })))
).includes("baki-1?w=1080"));

console.log("\n== TEST D: bir render icinde stabil ==");
const sig = (s) => JSON.stringify([...createImagePlan(SEC, s).entries()]);
check("eyni seed -> eyni plan (10 defa)",
  Array.from({ length: 10 }, () => sig(7)).every((x) => x === sig(7)));
check("PAGE_LOAD_SEED reqemdir", typeof PAGE_LOAD_SEED === "number");

console.log(fail === 0 ? `\nOK (${pass})` : `\nFAIL ${fail}/${pass}`);
process.exit(fail === 0 ? 0 : 1);
