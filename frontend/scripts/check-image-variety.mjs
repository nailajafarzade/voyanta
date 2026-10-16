/**
 * Şəkil seçimi YOXLAMASI — real backend məlumatı ilə.
 *
 * Bu skript yalnız frontend util-lərini (toPlace / createImagePlan) yoxlayır.
 * Heç bir backend kodu, endpoint və ya sxemi DƏYİŞMİR — sadəcə oxuyur.
 *
 * İstifadə:
 *   node scripts/check-image-variety.mjs
 *
 * Backend işləmirsə skript öz fixture məlumatından istifadə edir və
 * "offline" rejimini bildirir.
 */
import { toImageCandidates } from "../src/utils/imageCandidates.js";
import { toPlace } from "../src/utils/destinations.js";
import { createImagePlan, getPlanImage } from "../src/utils/imageAssignment.js";

const BASE = process.env.VOYANTA_API ?? "http://localhost:8080/api";

export const state = { passed: 0, failed: 0 };

export function check(label, condition, extra = "") {
  if (condition) {
    state.passed += 1;
    console.log(`  ok   ${label}`);
  } else {
    state.failed += 1;
    console.error(`  FAIL ${label}${extra ? ` — ${extra}` : ""}`);
  }
}

export async function fetchJson(path) {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) throw new Error(`${path} -> HTTP ${res.status}`);
  const body = await res.json();
  if (body.success !== true) throw new Error(`${path} -> ${body.errorCode}`);
  return body.data;
}

function u(id, n) {
  return `https://images.unsplash.com/photo-${id}-${n}?w=1080`;
}

/**
 * Backend cavabının real formaya uyğun fixture-ı.
 * `DestinationResponse.images[]` dəqiq bu formada qaytarılır
 * (bax: ImageCandidateResponse — id/url/fullUrl/photographer/photographerUrl/unsplashUrl)
 */
function fixtureDestination(id, name, country, tag, urls) {
  return {
    id,
    name,
    country,
    tag,
    images: urls.map((url, i) => ({
      id: `photo-${id}-${i}`,
      url,
      fullUrl: url.replace("w=1080", "w=1920"),
      photographer: `Photographer ${i}`,
      photographerUrl: `https://unsplash.com/@p${i}`,
      unsplashUrl: `https://unsplash.com/photos/${id}-${i}`,
    })),
  };
}

export const FIXTURES = {
  trending: [
    fixtureDestination("a1", "Maldives", "Maldiv adalari", "Deniz", [u("a1", 1), u("a1", 2), u("a1", 3)]),
    fixtureDestination("b1", "Santorini", "Yunanistan", "Deniz", [u("b1", 1), u("b1", 2)]),
    fixtureDestination("c1", "Misir", "Misir", "Tarix", [u("c1", 1), u("c1", 2), u("c1", 3)]),
  ],
  popular: [
    // MISIR — uc bolmede de var (ferqli sekil almali)
    fixtureDestination("c1", "Misir", "Misir", "Tarix", [u("c1", 1), u("c1", 2), u("c1", 3)]),
    fixtureDestination("d1", "Ismayilli", "Azerbaycan", "Tebiet", [u("d1", 1)]),
    fixtureDestination("e1", "Kapadokya", "Turkiye", "Tebiet", [u("e1", 1), u("e1", 2)]),
  ],
  featured: [
    fixtureDestination("b1", "Santorini", "Yunanistan", "Deniz", [u("b1", 1), u("b1", 2)]),
    // YALNIZ 1 namized -> tekrar istifade qebul edilir
    fixtureDestination("f1", "Yaponiya", "Yaponiya", "Medeniyyet", [u("f1", 1)]),
    // HEC namized yoxdur -> imageUrl fallback isle melidir
    { id: "g1", name: "Qeyri-movcud", country: "X", tag: "Tebiet", imageUrl: u("g1", 1), images: [] },
    // images[] bos, imageUrl da yox -> universal fallback
    { id: "h1", name: "Sekilsiz", country: "Y", tag: "Deniz", imageUrl: "", images: [] },
  ],
};

export const P = (list) => list.map(toPlace);

console.log("\nimageCandidates.js");
const m = toImageCandidates(FIXTURES.trending[0]);
check("images[] saxlanilir", m.length === 3, `got ${m.length}`);
check("backend sirasi qorunur", m[0].url === u("a1", 1) && m[2].url === u("a1", 3));
check("fullUrl gelir", m[0].fullUrl.includes("w=1920"));
check("attribution gelir", m[0].photographer === "Photographer 0");
check("images[] bos -> imageUrl namized olur", toImageCandidates(FIXTURES.featured[2]).length === 1);
check("hec nə yoxdursa bos siyahi", toImageCandidates(FIXTURES.featured[3]).length === 0);
check("tekrar URL-ler ayrilir",
  toImageCandidates({ images: [{ url: u("z", 1) }, { url: u("z", 1) }, { url: u("z", 2) }] }).length === 2);

console.log("\ntoPlace()");
const place = toPlace(FIXTURES.trending[0]);
check("images[] saxlanilir", Array.isArray(place.images) && place.images.length === 3);
check("imageUrl = images[0]", place.imageUrl === u("a1", 1));
check("namized yoxdursa imageUrl isle dusur", toPlace(FIXTURES.featured[2]).imageUrl === u("g1", 1));
check("hec nə yoxdursa imageUrl null", toPlace(FIXTURES.featured[3]).imageUrl === null);
check("kart dagilmir (title qalir)", Boolean(toPlace(FIXTURES.featured[3]).title));

console.log("\nCase 2 — eyni istiqamet, coxlu bolme");
const sections = [
  { key: "trending", places: P(FIXTURES.trending) },
  { key: "popular", places: P(FIXTURES.popular) },
  { key: "featured", places: P(FIXTURES.featured) },
];
const plan = createImagePlan(sections, 0);
const tMisr = getPlanImage(plan, "trending", P(FIXTURES.trending)[2]);
const pMisr = getPlanImage(plan, "popular", P(FIXTURES.popular)[0]);
check("Trending Misir != Popular Misir", tMisr?.url !== pMisr?.url);
check("Popular Misir Popular- bolmesinde", Boolean(pMisr?.url), pMisr?.url);
check("bolmede olmayan istiqamet null qaytarir",
  getPlanImage(plan, "featured", P(FIXTURES.trending)[2]) === null);

console.log("\nCase 3 — yalniz 1 namized");
const singlePlace = P([FIXTURES.featured[1]])[0];
check("tek namized secilir",
  getPlanImage(createImagePlan([{ key: "a", places: P([FIXTURES.featured[1]]) }]), "a", singlePlace)?.url === u("f1", 1));
const twice = createImagePlan([
  { key: "a", places: P([FIXTURES.featured[1]]) },
  { key: "b", places: P([FIXTURES.featured[1]]) },
]);
check("namized yoxdursa tekrar gosterilir (qebul edilir)",
  getPlanImage(twice, "a", singlePlace)?.url === u("f1", 1) &&
  getPlanImage(twice, "b", singlePlace)?.url === u("f1", 1));

console.log("\nCase 4 — namized yoxdur, kart qirilmir");
check("namizedsiz istiqamet plan bos qalir",
  getPlanImage(createImagePlan([{ key: "a", places: P([FIXTURES.featured[3]]) }]), "a", P([FIXTURES.featured[3]])[0]) === null);

console.log("\nCase 1 — ferqli istiqametler, tesadufi tekrar yoxdur");
const seen = new Map();
for (const s of sections) for (const p of s.places) {
  const img = getPlanImage(plan, s.key, p);
  if (img) seen.set(img.url, (seen.get(img.url) ?? 0) + 1);
}
check("eyni URL iki kartda tekarlanmir",
  [...seen.entries()].filter(([, n]) => n > 1).length === 0);

console.log("\nSabitlik (deterministiklik)");
const planJson = JSON.stringify([...plan.entries()]);
// EYNİ seed ile tutarlılıq (müqayisə üçün hər ikisində seed = 0)
check("eyni giris -> eyni netice",
  planJson === JSON.stringify([...createImagePlan(sections, 0).entries()]));
check("tekrar 10 defe cagirmaq dayismir",
  Array.from({ length: 10 }, () => JSON.stringify([...createImagePlan(sections, 0).entries()]))
    .every((r) => r === planJson));

console.log("\nMelumat mutasyasi");
const frozen = P(FIXTURES.trending);
const snapshot = JSON.stringify(frozen);
createImagePlan(sections);
check("giris obyektleri dayismir", JSON.stringify(frozen) === snapshot);

console.log("\n--- REAL BACKEND MElUMATI ---");
let offline = false;
let trendingReal = [];
let popularReal = [];
let featuredReal = [];

try {
  [trendingReal, popularReal, featuredReal] = await Promise.all([
    fetchJson("/destinations/trending?limit=6"),
    fetchJson("/destinations/popular?limit=6"),
    fetchJson("/destinations/featured?limit=12"),
  ]);
  console.log(`  backend alcatandir (${BASE})`);
} catch (error) {
  offline = true;
  console.log(`  backend alcatan deyil (${error.message}) — lokal fixture islədilir`);
}

if (!offline && trendingReal.length) {
  const realPlaces = {
    trending: trendingReal.map(toPlace),
    popular: popularReal.map(toPlace),
    featured: featuredReal.map(toPlace),
  };

  console.log(`\n  Trending (${trendingReal.length}):`);
  for (const d of trendingReal) {
    console.log(`    ${d.name.padEnd(20)} namized=${d.images?.length ?? 0}`);
  }

  const realSections = [
    { key: "trending", places: realPlaces.trending },
    { key: "popular", places: realPlaces.popular },
    { key: "featured", places: realPlaces.featured },
  ];
  const realPlan = createImagePlan(realSections);

  const byId = new Map();
  for (const [name, list] of Object.entries(realPlaces)) {
    for (const p of list) byId.set(p.id, [...(byId.get(p.id) ?? []), name]);
  }
  const repeated = [...byId.entries()].filter(([, names]) => names.length > 1);

  console.log("\n  Eyni istiqametin muxtelif bolmelerdeki sekilleri:");
  // Duzgun yoxlama: mumkun olan en COX ferqlilik.
  // Istiqametin N namizedi varsa ve M bolmede tekrarlanirsa,
  // maksimum ferqlilik = min(N, M) — hec ne ferqli sekil UYDURULMUR.
  let optimalVariety = true;
  for (const [id, names] of repeated) {
    const urls = names.map((name) =>
      getPlanImage(realPlan, name, realPlaces[name].find((p) => p.id === id))?.url
    );
    const distinct = new Set(urls).size;
    const sample = realPlaces.trending.find((p) => p.id === id);
    const candidateCount = sample?.images?.length ?? 0;
    const best = Math.min(candidateCount, names.length);
    if (distinct !== best) optimalVariety = false;
    const title = (sample?.title ?? id).padEnd(22);
    console.log(`    ${title} ${distinct}/${names.length} ferqli  (namized=${candidateCount}, mumkun=${best})`);
  }
  check("her istiqamet MUMKUN OLAN en cox ferqli sekili alir", optimalVariety);

  // Duzgun invariant: Təkrar yalnız ZƏRURƏT yarandıqda baş verir.
  // Yəni: hər istiqamət üçün fərqli şəkil sayı = min(namizəd sayı, tərsim sayı).
  // (1 namizədli istiqamət 3 bölmədə təkrarlanacaq — bu qəbul edilir.)
  let optimal = true;
  const byDest = new Map(); // id -> { total, distinct }
  for (const s of realSections) {
    for (const p of s.places) {
      const img = getPlanImage(realPlan, s.key, p);
      if (!img) continue;
      const entry = byDest.get(p.id) ?? { total: 0, distinct: new Set() };
      entry.total += 1;
      entry.distinct.add(img.url);
      byDest.set(p.id, entry);
    }
  }
  for (const [destId, entry] of byDest) {
    const all = realSections.flatMap((s) => s.places);
    const sample = all.find((p) => p.id === destId);
    const best = Math.min(sample?.images?.length ?? 1, entry.total);
    if (entry.distinct.size !== best) { optimal = false; }
  }
  check("tekrar yalniz ZƏRURƏT yarandiqda bas verir", optimal);

  // Relevance qorunur: her sekil oz istiqametinin images[] siyahisindandir
  let fromOwnList = true;
  for (const s of realSections) {
    for (const p of s.places) {
      const img = getPlanImage(realPlan, s.key, p);
      if (img && !p.images.some((c) => c.url === img.url)) fromOwnList = false;
    }
  }
  check("her sekil oz istiqametinin namized siyahisindandir (relevans qorunur)", fromOwnList);

  check("real datada da stabildir",
    JSON.stringify([...realPlan.entries()]) ===
      JSON.stringify([...createImagePlan(realSections).entries()]));
}

console.log(
  state.failed === 0
    ? `\n✅ Butun yoxlamalar kecdi (${state.passed} ok)\n`
    : `\n❌ ${state.failed} yoxlama kecmədi (${state.passed} ok)\n`
);
process.exit(state.failed === 0 ? 0 : 1);

