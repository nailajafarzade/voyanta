import api, { unwrap } from "./axios";

/** GET /api/destinations/featured — auth tələb olunmur, opsional `limit`. */
export async function getFeaturedDestinations(limit) {
    const response = await api.get("/destinations/featured", {
        params: limit ? { limit } : undefined,
    });
    return unwrap(response) ?? [];
}

/** GET /api/destinations/{id} — auth tələb olunmur. */
export async function getDestinationById(id) {
    const response = await api.get(`/destinations/${id}`);
    return unwrap(response);
}

/** GET /api/destinations/batch?ids=... — auth tələb olunmur. */
export async function getDestinationsByIds(ids) {
    if (!Array.isArray(ids) || ids.length === 0) return [];
    const response = await api.get("/destinations/batch", {
        params: { ids },
    });
    return unwrap(response) ?? [];
}

/**
 * GET /api/destinations/popular — açıq endpoint.
 *
 * Ən çox favoriləşdirilən istiqamətlər. Reytinq yalnız backend-də hesablanır;
 * "populyarlıq uyğunluğu üstə keçmir" qaydası server tərəfdədir.
 */
export async function getPopularDestinations(limit, exclude) {
    const response = await api.get("/destinations/popular", {
        params: {
            ...(limit ? { limit } : {}),
            ...(exclude?.length ? { exclude } : {}),
        },
    });
    return unwrap(response) ?? [];
}
