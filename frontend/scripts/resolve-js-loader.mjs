/**
 * Yalnız test üçün: Vite `.js` uzantısını tələb etmir, Node ESM isə tələb edir.
 * Bu loader lokal `from "./x"` import-larına `.js` əlavə edir.
 * Mənbə kodu DƏYİŞMİR — Vite ilə build hələ də adi yolla işləyir.
 */
import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";

export async function resolve(specifier, context, nextResolve) {
  if (specifier.startsWith(".") && !/\.[a-z]+$/i.test(specifier)) {
    const withExt = new URL(`${specifier}.js`, context.parentURL);
    if (existsSync(fileURLToPath(withExt))) {
      return nextResolve(withExt.href, context);
    }
  }
  return nextResolve(specifier, context);
}
