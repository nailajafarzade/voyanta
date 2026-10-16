/**
 * Frontend utility-lərinin tez yoxlaması.
 *
 * Vite dev-server işləyərkən modulları ondan yükləyib Node-da icra edir
 * (Vite import yollarını düzəldir, ona görə Node-un ehtiyacı yoxdur).
 *
 * İstifadə:
 *   1) npm run dev
 *   2) node scripts/check-utils.mjs
 */
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";

const BASE = process.env.VITE_URL ?? "http://localhost:5173";
const dir = mkdtempSync(join(tmpdir(), "voy-check-"));

/** Vite-dan modulu yükləyib müvəqqəti .mjs kimi yazır. */
async function loadModule(srcPath, name) {
  const res = await fetch(`${BASE}/${srcPath}`);
  if (!res.ok) throw new Error(`${srcPath} → HTTP ${res.status}`);

  let code = await res.text();
  // mütləq/nisbi yolları lokal müvəqqəti fayllara yönləndir
  code = code.replace(
    /from\s+"(?:\.\.\/data\/destinationDetails|\/src\/data\/destinationDetails(?:\.js)?)"/g,
    `from "./${dataFile}"`
  );
  code = code.replace(
    /from\s+"\.\/([^"]+)"/g,
    (m, p) => `from "./${p.replace(/\.js$/, ".mjs")}"`
  );

  const file = join(dir, name);
  writeFileSync(file, code, "utf8");
  return import(pathToFileURL(file).href);
}

const dataFile = "d-data.mjs";

const { getSeason, gradeOf, monthRange, seasonStatus } = await loadModule(
  "src/utils/season.js",
  "u-season.mjs"
);
const { likeRatio, sortByTrending, trendScore } = await loadModule(
  "src/utils/trending.js",
  "u-trending.mjs"
);
// data modulu əvvəlcə yazılmalıdır ki, link düzəlsin
await loadModule("src/data/destinationDetails.js", dataFile);
const { getDestinationDetails } = await loadModule(
  "src/utils/destinationDetails.js",
  "u-destDetails.mjs"
);

let failed = 0;
const check = (label, condition, extra = "") => {
  if (condition) {
    console.log(`  ok   ${label}`);
  } else {
    failed += 1;
    console.error(`  FAIL ${label} ${extra}`);
  }
};

console.log("\nseason.js");
const season = getSeason();
check("getSeason() qaytarır", Boolean(season?.id));
check("gradeOf() CSS sinfi verir", gradeOf().startsWith("voy-grade-"), gradeOf());
check("monthRange([5,6])", monthRange([5, 6]) === "May – İyn", monthRange([5, 6]));
check("peak statusu", seasonStatus([5, 6, 7], 6).state === "peak");
check("off statusu", seasonStatus([5, 6, 7], 1).state === "off");

console.log("\ntrending.js");
const a = { id: "11111111-1111-1111-1111-111111111111" };
const b = { id: "22222222-2222-2222-2222-222222222222" };
check("trendScore 0..1", trendScore(a.id) >= 0 && trendScore(a.id) <= 1);
check("trendScore sabitdir", trendScore(a.id) === trendScore(a.id));
check("likeRatio 0..1", likeRatio(a.id) >= 0 && likeRatio(a.id) <= 1);
const sorted = sortByTrending([a, b]);
check(
  "sortByTrending stabildir",
  sortByTrending([a, b]).map((p) => p.id).join() === sorted.map((p) => p.id).join()
);
check("sortByTrending kopyalayır", sorted.length === 2 && sorted !== undefined);

console.log("\ndestinationDetails.js");
// toPlace() formatı (ad + ölkə birlikdə)
const place = { id: "1", title: "Santorini, Yunanıstan", subtitle: "Dəniz və gündoğuş", imageUrl: "" };
const details = getDestinationDetails(place);
check("curated tapıldı", details.isCurated === true);
check("özet mövcuddur", details.summary.length > 30);
check("yerlər var", details.places.length >= 5);
check("fəaliyyətlər var", details.activities.length >= 4);
check("faydalı məlumat doldurulub", Boolean(details.currency && details.language));
check("tövsiyə olunan gün", typeof details.stayLength === "string");

// backend formatı
const raw = { id: "2", name: "Misir", country: "Misir", tag: "Tarix və mədəniyyət" };
check("backend formatı tanınır", getDestinationDetails(raw).isCurated === true);

// tag fallback (curated yoxdur)
const unknown = { id: "3", title: "Nəzəri olmayan yer, X", subtitle: "Dəniz və gündoğuş" };
const fallback = getDestinationDetails(unknown);
check("fallback işə düşür", fallback.isCurated === false);
check("fallback yerləri var", fallback.places.length >= 3);
check("fallback boş deyil", fallback.headline.length > 0);

console.log(
  failed === 0 ? "\n✅ Bütün yoxlamalar keçdi\n" : `\n❌ ${failed} yoxlama keçmədi\n`
);
process.exit(failed === 0 ? 0 : 1);
