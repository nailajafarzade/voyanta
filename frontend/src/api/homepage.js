import api, { unwrap } from "./axios";
/** GET /api/homepage/stats — auth tələb olunmur. */
export async function getHomepageStats() {
    return unwrap(await api.get("/homepage/stats"));
}
