/**
 * Səhifə üzrə şəkil TƏYİNİ — NƏZARƏTLİ ROTASİYA, hər render DƏYİŞMİR.
 *
 * PROBLEM: backend hər istiqamət üçün bir neçə relevant şəkil qaytarır, amma
 * eyni istiqamət Popular / "Sənə xüsusi" bölmələrində təkrarlanır.
 *
 * HƏLL: `createImagePlan()` bütün bölmələri BİR YERDƏ baxıb hər karta
 * deterministik bir namizəd təyin edir. Prioritet qaydası:
 *
 *   1. RELEVANS  — namizədlər backend sırasıyla gəlir, sıra POZULMUR
 *   2. ROTASİYA  — hər səhifə yüklənməsində başlanğıc nöqəm dəyişir ki,
 *                   səhifəni yenilədikdə başqa şəkil görünsün
 *   3. MÜXTƏLİFLİK — eyni URL başqa bölmədə işlədilibsə, növbəti
 *      (amma hələ RELEVANT) namizəd seçilir
 *
 * NİYƏ `Math.random()` YOXDUR:
 * `Math.random()` React render zamanında çağırılsa, hər re-render şəkli
 * dəyişərdi. Burada əvəzinə `PAGE_LOAD_SEED` modul səviyyəsində BİR DƏFƏ
 * hesablanır: yeni səhifə yüklənməsində dəyişir, eyni səhifədə SABİT qalır.
 * Bütün seçim bu seed-dən TAYİN olunur → bir render içində təsadüf yoxdur.
 *
 * Bu funksiya SAFDIR (pure): heç bir istiqamət obyektini MUTASIYA ETMİR,
 * heç bir global state yaratmır — yalnız öz Map-i qaytarır.
 */

/** FNV-1a 32-bit hash — sabit (təsadüfi deyil), seed ilə birləşdirmək üçün. */
function hash32(value) {
  let hash = 0x811c9dc5;
  const text = String(value ?? "");
  for (let i = 0; i < text.length; i += 1) {
    hash ^= text.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  return hash >>> 0;
}

let pageLoadCounter = 0;

/**
 * Səhifə yüklənməsi səviyyəsində BİR DƏFƏ hesablanan seed.
 *
 * Modul yükləndikdə bir dəfə işə düşür → bütün səhifə ərzində sabit qalır
 * (React istənilən qədər re-render etsə də şəkil dəyişmir).
 * Səhifə yeniləndikdə modul yenidən yüklənir və seed dəyişir → namizəd
 * növbəti sıradan başlayır.
 */
function buildPageLoadSeed() {
  pageLoadCounter += 1;
  return hash32(`${Date.now()}:${pageLoadCounter}`);
}

export const PAGE_LOAD_SEED = buildPageLoadSeed();

/** Bölmə adı + destination id → plan açarı. */
export function imagePlanKey(sectionKey, place) {
  return `${sectionKey}::${place?.id ?? ""}`;
}

/**
 * Bütün səhifə bölmələri üçün şəkil planı qurur.
 *
 * @param {Array<{ key: string, places?: Array<{ id: string, images?: Array }> }>} sections
 *   Bölmələr EKRANDA GÖRÜNÜM SİRASI ilə verilir. Bu, "qalib bölmə" qaydasını
 *   müəyyən edir: yuxarıdakı bölmələr ən yaxşı şəkilləri alır. Yan-yana bölmə
 *   sonradan yüklənsə belə, yuxarıdakı bölmənin seçimi DƏYİŞMİR.
 * @param {number} [seed] Safix yuklenme seed-i. Defolt: `PAGE_LOAD_SEED`
 *   (modul seviyesinde bir defa hesablanir). Testler degisen seed verebilir.
 * @returns {Map<string, object>}
 */
export function createImagePlan(sections, seed = PAGE_LOAD_SEED) {
  const plan = new Map();
  /** Səhifə üzrə artıq göstərilmiş bütün URL-lər. */
  const usedUrls = new Set();
  /** destinationId → o istiqamətdə artıq işlədilmiş namizəd indeksləri. */
  const usedCandidates = new Map();
  /** Eyni istiqamət eyni bölmədə təkrar gələ bilər → növbəti açar. */
  const occurrence = new Map();

  // Seed menfi/NaN ola bilmez - uint32-ye normallashdirilir (tesadufi DEYIL)
  const safeSeed = (Math.trunc(Number(seed)) || 0) >>> 0;

  for (const section of sections ?? []) {
    const sectionKey = section?.key;
    if (!sectionKey) continue;

    for (const place of section?.places ?? []) {
      const baseKey = imagePlanKey(sectionKey, place);
      const seen = occurrence.get(baseKey) ?? 0;
      occurrence.set(baseKey, seen + 1);
      const key = seen === 0 ? baseKey : `${baseKey}#${seen}`;

      const candidates = Array.isArray(place?.images) ? place.images : [];

      // Namizəd yoxdursa plan boş qalır → DestinationImage `place.imageUrl`-ə,
      // o da yoxdursa universal fallback-ə keçir (kart heç vaxt qırılmır).
      if (candidates.length === 0) continue;

      const takenForPlace = usedCandidates.get(place?.id) ?? new Set();

      // ROTASIYA: bu safix yuklenmesi ucun baslangic noqem.
      // Her safix yuklenmesinde deyisir -> yenilemede basqa namized gorunur.
      // Eyni safixde SABITDIR -> render erzinde sekil qopmur.
      const start = (hash32(place?.id) + safeSeed) >>> 0;
      const offset = start % candidates.length;

      // 1) Rotasiya noqesinden baslayaraq en yaxsi HEL ISTIFADE OLUNMAMIS
      //    namededi gotur. Sira pozulmur - dovri sekilde yalniz baslangic deyisir.
      let index = -1;
      for (let step = 0; step < candidates.length; step += 1) {
        const i = (offset + step) % candidates.length;
        if (candidates[i]?.url && !usedUrls.has(candidates[i].url)) {
          index = i;
          break;
        }
      }

      // 2) Namizədlərin hamısı işlədilibsə → həmin istiqamətin növbəti
      //    (hələ istifadə olunmamış) namizədi. Beləcə eyni istiqamət
      //    Popular → Sənə xüsusi kimi sıra ilə fərqli şəkil göstərir.
      if (index === -1) {
        index = candidates.findIndex((_, i) => !takenForPlace.has(i));
      }

      // 3) Hər şey təkrar istifadə olunub (məs. yalnız 1 namizəd var) →
      //    ən yaxşı, yəni ən çox uyğun namizəd yenidən göstərilir.
      if (index === -1) index = 0;

      const chosen = candidates[index];
      if (chosen?.url) {
        usedUrls.add(chosen.url);
        takenForPlace.add(index);
        usedCandidates.set(place?.id, takenForPlace);
      }

      plan.set(key, chosen);
    }
  }

  return plan;
}

/**
 * Plan-dan istiqamətin bu bölmədəki şəklini götürür.
 * Plan boşdursa `null` → komponent mövcud `imageUrl` fallback-inə keçir.
 */
export function getPlanImage(plan, sectionKey, place) {
  if (!plan || !place) return null;
  return plan.get(imagePlanKey(sectionKey, place)) ?? null;
}
