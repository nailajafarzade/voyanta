import api, { unwrap } from "./axios";
/** GET /api/users/me — auth tələb olunur. */
export async function getMe() {
    return unwrap(await api.get("/users/me"));
}
/** PUT /api/users/me — yalnız fullName və phone dəyişdirilə bilir (email yox). */
export async function updateMe(payload) {
    return unwrap(await api.put("/users/me", payload));
}
