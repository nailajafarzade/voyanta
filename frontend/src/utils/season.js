/**
 * Mövsüm məntəqi — yalnız frontend, backend API-yə toxunmur.
 *
 * `getSeason()` cari mövsümü (Sxemi qışdan başlayır) qaytarır.
 * `gradeOf()` şəklin üzərindəki CSS filtr/qat sinfini verir ki, vizual
 * mövsümə uyğun görünsün. `bestMonthsOf()` isə istiqamətin ən yaxşı
 * səyahət aylarını bildirir (curated məlumat yoxdursa `null`).
 */

export const SEASONS = {
  spring: {
    id: "spring",
    label: "Bahar",
    icon: "🌱",
    months: [3, 4, 5],
    blurb: "Çiçəklənmə, yumşaq hava və təzə günəş — gəzintilər üçün əlverişli mövsüm.",
    grade: "voy-grade-spring",
    accent: "#3dbe7a",
    tint: "bg-mint-50",
    text: "text-mint-600",
    border: "border-mint-200",
    badge: "bg-mint-500",
  },
  summer: {
    id: "summer",
    label: "Yay",
    icon: "☀️",
    months: [6, 7, 8],
    blurb: "İsti dəniz, uzun günlər və açıq hava fəaliyyətləri üçün ən aktiv mövsüm.",
    grade: "voy-grade-summer",
    accent: "#f2c230",
    tint: "bg-sun-50",
    text: "text-sun-700",
    border: "border-sun-200",
    badge: "bg-sun-500",
  },
  autumn: {
    id: "autumn",
    label: "Payız",
    icon: "🍂",
    months: [9, 10, 11],
    blurb: "Sərhəddən keçmək üçün əlverişli — az kütlə, sərin günlər və rəngli təbəssüm.",
    grade: "voy-grade-autumn",
    accent: "#f59e0b",
    tint: "bg-amber-50",
    text: "text-amber-700",
    border: "border-amber-200",
    badge: "bg-amber-500",
  },
  winter: {
    id: "winter",
    label: "Qış",
    icon: "❄️",
    months: [12, 1, 2],
    blurb: "Sakit günlər, endirimlər və isti içkilər üçün ən səhmətli mövsüm.",
    grade: "voy-grade-winter",
    accent: "#60a5fa",
    tint: "bg-sky-50",
    text: "text-sky-700",
    border: "border-sky-200",
    badge: "bg-sky-500",
  },
};

export const SEASON_ORDER = ["spring", "summer", "autumn", "winter"];

const AZ_MONTHS_SHORT = [
  "Yan",
  "Fev",
  "Mar",
  "Apr",
  "May",
  "İyn",
  "İyl",
  "Avq",
  "Sen",
  "Okt",
  "Noy",
  "Dek",
];

/** Cari mövsümü qaytarır (default: bu gün). */
export function getSeason(date = new Date()) {
  const month = date.getMonth() + 1;
  const found = SEASON_ORDER.find((id) => SEASONS[id].months.includes(month));
  return SEASONS[found];
}

/** Mövsüm üzrə CSS filtr/qat sinfi. */
export function gradeOf(season) {
  return (season ?? getSeason()).grade;
}

/** "noyabr" */
export function monthName(month) {
  return AZ_MONTHS_SHORT[month - 1] ?? "";
}

/** [5,6,7,8,9] → "May – Senyabr" */
export function monthRange(months) {
  if (!Array.isArray(months) || months.length === 0) return null;
  if (months.length === 1) return monthName(months[0]);
  return `${monthName(months[0])} – ${monthName(months[months.length - 1])}`;
}

/**
 * İstiqamətin ən yaxşı səyahət ayları (curated məlumatdan).
 * @param {number[]|null} bestMonths
 */
export function seasonStatus(bestMonths, month = new Date().getMonth() + 1) {
  if (!Array.isArray(bestMonths) || bestMonths.length === 0) {
    return { state: "unknown", label: "İl olinə görə", tone: "neutral" };
  }
  if (bestMonths.includes(month)) {
    return { state: "peak", label: "İndi ən yaxşı vaxtdır", tone: "good" };
  }
  // Ətraf aylar (qonşu ay) — "yaxşı vaxt"
  const neighbours = [month - 1, month + 1].filter((m) => m >= 1 && m <= 12);
  if (neighbours.some((m) => bestMonths.includes(m))) {
    return { state: "good", label: "Yaxşı vaxt", tone: "good" };
  }
  return { state: "off", label: "Kəsirli mövsüm", tone: "muted" };
}
