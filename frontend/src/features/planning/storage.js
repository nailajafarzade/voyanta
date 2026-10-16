/**
 * Sorğu sessiyası və anonim plan üçün lokal yaddaş.
 *
 * - surveySessionId: çoxaddımlı sorğu ayrı-ayrı route-lardadır (/plan1../plan7),
 *   ona görə addımlar arasında sessiya id-si burada saxlanılır. Cavabların özü
 *   backend-də (Redis, 30 dəq) qalır.
 * - pendingPlanId: anonim istifadəçi plan yaradanda saxlanılır; daxil olduqdan
 *   sonra həmin plan claim edilir.
 */
const SURVEY_SESSION_KEY = "voyanta.surveySessionId";
const PENDING_PLAN_KEY = "voyanta.pendingPlanId";
const CLAIMED_PLANS_KEY = "voyanta.claimedPlanIds";
function read(key) {
    try {
        return window.localStorage.getItem(key);
    }
    catch {
        return null;
    }
}
function write(key, value) {
    try {
        window.localStorage.setItem(key, value);
    }
    catch {
        // storage bloklanıbsa axını dayandırmırıq
    }
}
function remove(key) {
    try {
        window.localStorage.removeItem(key);
    }
    catch {
        // yuxarıdakı kimi
    }
}
export const getStoredSurveySessionId = () => read(SURVEY_SESSION_KEY);
export const setStoredSurveySessionId = (id) => write(SURVEY_SESSION_KEY, id);
export const clearStoredSurveySessionId = () => remove(SURVEY_SESSION_KEY);
export const getPendingPlanId = () => read(PENDING_PLAN_KEY);
export const setPendingPlanId = (id) => write(PENDING_PLAN_KEY, id);
export const clearPendingPlanId = () => remove(PENDING_PLAN_KEY);
/** Eyni planı təkrar-təkrar claim etməmək üçün. */
export function getClaimedPlanIds() {
    const raw = read(CLAIMED_PLANS_KEY);
    if (!raw)
        return [];
    try {
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed.filter((id) => typeof id === "string") : [];
    }
    catch {
        return [];
    }
}
export function markPlanClaimed(planId) {
    const claimed = getClaimedPlanIds();
    if (!claimed.includes(planId)) {
        write(CLAIMED_PLANS_KEY, JSON.stringify([...claimed, planId]));
    }
}
export function isPlanClaimed(planId) {
    return getClaimedPlanIds().includes(planId);
}
