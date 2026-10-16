import { DESTINATION_DETAILS } from "../data/destinationDetails";

/**
 * Backend `tag` mətninə görə generik ətraflı məlumat.
 * Curated məlumat olmayan istiqamətlər üçün heç boş yer qalmır.
 */
const TAG_FALLBACKS = [
  {
    match: /dəniz|gündoğuş|günəş|dəniz/i,
    headline: "Dəniz, gün batımı və qumlu sahillər.",
    places: [
      { name: "Mərkəzi sahil", note: "Gün batımı üçün ən yaxşı baxış nöqtəsi." },
      { name: "Köhnə şəhər", note: "Dar küçəklər və tarixi memarlıq." },
      { name: "Dəniz kənarı", note: "Qərb günəşi və sakit sahil." },
    ],
    activities: [
      { icon: "🏖️", label: "Sahil günü" },
      { icon: "🌅", label: "Gün batımı" },
      { icon: "🚤", label: "Qayıq gəzintisi" },
      { icon: "🤿", label: "Sualtı müşahidə" },
    ],
  },
  {
    match: /tarix|mədəniyyət|muzey|qala/i,
    headline: "Tarixi abidələr və zəngin mədəniyyət.",
    places: [
      { name: "Şəhər mərkəzi", note: "Əsas tarixi abidələrin yığılma məntəqəsi." },
      { name: "Qala və muzey", note: "Regionun ən böyük memarlıq abidəsi." },
      { name: "Yerli bazar", note: "Gündəlik həyatı və yerli məhsulları görmək üçün." },
    ],
    activities: [
      { icon: "🏛️", label: "Muzey ziyarəti" },
      { icon: "📸", label: "Memarlıq foto-tur" },
      { icon: "🍽️", label: "Yerli xörək" },
      { icon: "🚶", label: "Şəhər gəzintisi" },
    ],
  },
  {
    match: /təbiət|dağ|çay|təbiiət|meşə/i,
    headline: "Təbiət, meşə və açıq hava.",
    places: [
      { name: "Milli park", note: "Meşə yolları və nəzarət nöqtələri." },
      { name: "Göl / çay sahili", note: "Sakit piknik və gəzinti üçün." },
      { name: "Mənzərə nöqtəsi", note: "Ətrafın ən yaxşı panoramı." },
    ],
    activities: [
      { icon: "🥾", label: "Yürüş marşrutu" },
      { icon: "🏕️", label: "Kampinq" },
      { icon: "📷", label: "Təbiət fotoşəkli" },
      { icon: "🚲", label: "Velosiped" },
    ],
  },
];

function normalise(value) {
  return String(value ?? "")
    .toLowerCase()
    .replace(/ı/g, "i")
    .replace(/ə/g, "e")
    .replace(/ö/g, "o")
    .replace(/ü/g, "u")
    .replace(/ş/g, "s")
    .replace(/ç/g, "c")
    .replace(/ğ/g, "g")
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

/** "Santorini" / "maldiv adalari" → açar. Həm backend, həm də `toPlace` formatını qəbul edir. */
function lookupKey(destination) {
  const name = normalise(
    destination?.name ?? destination?.title?.split(",")[0] ?? ""
  );
  const country = normalise(
    destination?.country ?? destination?.title?.split(",").slice(1).join(",") ?? ""
  );
  const candidates = [name, `${name} ${country}`.trim()].filter(Boolean);

  for (const key of Object.keys(DESTINATION_DETAILS)) {
    const flat = normalise(key);
    if (candidates.some((value) => value.includes(flat) || flat.includes(value))) {
      return key;
    }
  }
  return null;
}

function tagFallback(tag) {
  const text = String(tag ?? "");
  const match = TAG_FALLBACKS.find((entry) => entry.match.test(text));
  if (match) return match;
  return {
    headline: "Seçilmiş səyahət istiqaməti.",
    places: [
      { name: "Mərkəzi hissə", note: "İstiqamətin ən tanınmış nöqtəsi." },
      { name: "Tarixi mərkəz", note: "Yerli abidələr və memarlıq." },
      { name: "Təbiiət guşəsı", note: "Sakit gəzinti üçün ideal." },
    ],
    activities: [
      { icon: "🚶", label: "Şəhər gəzintisi" },
      { icon: "📷", label: "Foto yürüş" },
      { icon: "🍽️", label: "Yerli mətbəx" },
      { icon: "🗺️", label: "Mərkəzi bazar" },
    ],
  };
}

/**
 * Backend destination + curated məlumatı birləşdirir.
 * @param {{id:string,name:string,country:string,tag:string}} destination
 */
export function getDestinationDetails(destination) {
  const key = lookupKey(destination);
  const curated = key ? DESTINATION_DETAILS[key] : null;
  const fallback = tagFallback(destination?.tag ?? destination?.subtitle);

  return {
    isCurated: Boolean(curated),
    headline: curated?.headline ?? fallback.headline,
    summary: curated?.summary ?? "",
    bestMonths: curated?.bestMonths ?? null,
    bestSeason: curated?.bestSeason ?? null,
    stayLength: curated?.stayLength ?? "2–4 gün",
    budget: curated?.budget ?? "Orta",
    language: curated?.language ?? "Yerli dil",
    currency: curated?.currency ?? "Yerli valyuta",
    timezone: curated?.timezone ?? null,
    facts:
      curated?.facts ??
      [
        { label: "Tövsiyə olunan gün", value: "2 – 4 gün" },
        { label: "Xərc səviyyəsi", value: "Orta" },
        { label: "Maraq", value: destination?.tag ?? destination?.subtitle ?? "Səyahət" },
      ],
    places: curated?.places ?? fallback.places,
    activities: curated?.activities ?? fallback.activities,
    tips: curated?.tips ?? [],
  };
}

/** Səhifə başlığı üçün qısa tək cümlə. */
export function shortBlurb(destination) {
  const details = getDestinationDetails(destination);
  return details.headline;
}
