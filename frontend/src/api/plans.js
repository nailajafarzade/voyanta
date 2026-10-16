import api, { unwrap } from "./axios";
/**
 * POST /api/plans/generate — auth opsionaldır, HTTP 202 qaytarır.
 * Cavabın `data` sahəsi birbaşa plan UUID-sidir (obyekt deyil).
 */
export async function generatePlan(surveySessionId) {
    return unwrap(await api.post("/plans/generate", { surveySessionId }));
}
/** GET /api/plans/{id}/status — polling üçün. */
export async function getPlanStatus(planId) {
    return unwrap(await api.get(`/plans/${planId}/status`));
}
/** GET /api/plans/{id} — auth opsionaldır; sahibi olmayan üçün kilidli günlərin items-i null gəlir. */
export async function getPlan(planId) {
    return unwrap(await api.get(`/plans/${planId}`));
}
/** POST /api/plans/{id}/claim — auth tələb olunur, boş body. */
export async function claimPlan(planId) {
    await api.post(`/plans/${planId}/claim`);
}
