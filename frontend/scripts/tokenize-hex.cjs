/**
 * Tailwind arbitrary dəyərlərini dizayn tokenləri ilə əvəz edir:
 *   bg-[#5B8DEF] → bg-brand-500, hover:bg-[#4A7CE0] → hover:bg-brand-600, ...
 * Dəyişiklik heç bir məntiqi toxunmur — yalnız görünən rəng kodlarını
 * `@theme` tokenləri ilə eyniləşdirir.
 *
 * İstifadə: node scripts/tokenize-hex.cjs
 */
const { readFileSync, writeFileSync, readdirSync, statSync } = require("node:fs");
const { join } = require("node:path");

const SRC = join(__dirname, "..", "src");

const REPLACEMENTS = [
  [/bg-\[#5B8DEF\]/g, "bg-brand-500"],
  [/hover:bg-\[#4A7CE0\]/g, "hover:bg-brand-600"],
  [/active:bg-\[#396BD0\]/g, "active:bg-brand-700"],
  [/text-\[#5B8DEF\]/g, "text-brand-500"],
  [/hover:text-\[#4A7CE0\]/g, "hover:text-brand-600"],
  [/border-\[#5B8DEF\]/g, "border-brand-500"],
  [/border-t-\[#5B8DEF\]/g, "border-t-brand-500"],
  [/focus:border-\[#5B8DEF\]/g, "focus:border-brand-500"],
  [/bg-\[#F9FAFB\]/g, "bg-canvas"],
  [/bg-\[#1D1E22\]/g, "bg-ink-900"],
  [/ring-\[#1D1E22\]/g, "ring-ink-900"],
  [/ring-offset-\[#1D1E22\]/g, "ring-offset-ink-900"],
  [/focus-visible:ring-\[#5B8DEF\]/g, "focus-visible:ring-brand-500"],
  [/blue-50\/70/g, "brand-50/70"],
  [/blue-50\/50/g, "brand-50/50"],
  [/\bbg-blue-50\/50\b/g, "bg-brand-50/50"],
  [/\bborder-blue-100\b/g, "border-brand-100"],
  [/\bbg-blue-50\/70\b/g, "bg-brand-50/70"],
  [/\bbg-blue-50\b/g, "bg-brand-50"],
  [/\btext-blue-\d{3}\b/g, "text-brand-600"],
];

function walk(dir) {
  return readdirSync(dir).flatMap((name) => {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) return walk(full);
    return /\.(jsx?|css)$/.test(name) ? [full] : [];
  });
}

let changed = 0;

for (const file of walk(SRC)) {
  const before = readFileSync(file, "utf8");
  let after = before;

  for (const [pattern, value] of REPLACEMENTS) after = after.replace(pattern, value);

  if (after !== before) {
    writeFileSync(file, after, "utf8");
    changed += 1;
    console.log(`dəyişdi: ${file.replace(SRC, "src")}`);
  }
}

console.log(`\n${changed} fayl yeniləndi.`);
