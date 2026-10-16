/** Tarix/məbləğ formatlaması — backend ISO tarix (yyyy-MM-dd) və ədəd qaytarır. */
const AZ_MONTHS = [
    "Yanvar",
    "Fevral",
    "Mart",
    "Aprel",
    "May",
    "İyun",
    "İyul",
    "Avqust",
    "Sentyabr",
    "Oktyabr",
    "Noyabr",
    "Dekabr",
];
const AZ_WEEKDAYS = [
    "Bazar",
    "Bazar ertəsi",
    "Çərşənbə axşamı",
    "Çərşənbə",
    "Cümə axşamı",
    "Cümə",
    "Şənbə",
];
function parseIsoDate(value) {
    if (!value)
        return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
}
/** "2026-05-12" -> "12 May, 2026" */
export function formatDate(value) {
    const date = parseIsoDate(value);
    if (!date)
        return null;
    return date.getUTCDate() + " " + AZ_MONTHS[date.getUTCMonth()] + ", " + date.getUTCFullYear();
}
/** "2026-05-12" -> "12 May, Bazar ertəsi" */
export function formatDayLabel(value) {
    const date = parseIsoDate(value);
    if (!date)
        return null;
    return date.getUTCDate() + " " + AZ_MONTHS[date.getUTCMonth()] + ", " + AZ_WEEKDAYS[date.getUTCDay()];
}
/** Eyni ay olanda "12-16 May, 2026", əks halda tam iki tarix. */
export function formatDateRange(start, end) {
    const startDate = parseIsoDate(start);
    const endDate = parseIsoDate(end);
    if (!startDate || !endDate)
        return formatDate(start) ?? formatDate(end);
    const sameMonth = startDate.getUTCMonth() === endDate.getUTCMonth() &&
        startDate.getUTCFullYear() === endDate.getUTCFullYear();
    if (sameMonth) {
        return (startDate.getUTCDate() +
            "-" +
            endDate.getUTCDate() +
            " " +
            AZ_MONTHS[startDate.getUTCMonth()] +
            ", " +
            startDate.getUTCFullYear());
    }
    return formatDate(start) + " - " + formatDate(end);
}
/** Planın N-ci gününün tarixi (startDate varsa). */
export function dateForDayNumber(startDate, dayNumber) {
    const date = parseIsoDate(startDate);
    if (!date)
        return null;
    date.setUTCDate(date.getUTCDate() + (dayNumber - 1));
    return date.toISOString().slice(0, 10);
}
/** 650 -> "650 AZN"; null/undefined -> "—" */
export function formatAzn(value) {
    if (value === null || value === undefined || Number.isNaN(value))
        return "—";
    return Math.round(value).toLocaleString("en-US") + " AZN";
}
/** 12000 -> "12,000+" */
export function formatCount(value) {
    if (value === null || value === undefined || Number.isNaN(value))
        return "—";
    return value.toLocaleString("en-US") + "+";
}
/** 4.9 -> "4.9/5" */
export function formatRating(value) {
    if (value === null || value === undefined || Number.isNaN(value))
        return "—";
    return value.toFixed(1) + "/5";
}
/** "Aysel Məmmədova" -> "AM" */
export function initialsOf(fullName) {
    if (!fullName)
        return "";
    return fullName
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0).toLocaleUpperCase("az-AZ"))
        .join("");
}
