import api, { unwrap } from "./axios";
/** POST /api/survey/sessions — auth tələb olunmur, boş body. Redis-də 30 dəqiqə yaşayır. */
export async function createSession() {
    return unwrap(await api.post("/survey/sessions"));
}
/** GET /api/survey/sessions/{id} — sessiya vaxtı bitibsə 404 RESOURCE_NOT_FOUND. */
export async function getSession(sessionId) {
    return unwrap(await api.get(`/survey/sessions/${sessionId}`));
}
/** PATCH /api/survey/sessions/{id} — yalnız cari addımın sahələri göndərilir. */
export async function updateSession(sessionId, patch) {
    return unwrap(await api.patch(`/survey/sessions/${sessionId}`, patch));
}
