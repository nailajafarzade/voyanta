import api, { unwrap } from "./axios";
/**
 * GET /api/recommendations/personalized — AUTH TƏLƏB OLUNUR.
 * Yalnız daxil olmuş istifadəçi üçün çağırılmalıdır; tarixçəsi olmayan
 * istifadəçiyə backend featured siyahısını qaytarır.
 */
export async function getPersonalizedDestinations(limit) {
    const response = await api.get("/recommendations/personalized", {
        params: limit ? { limit } : undefined,
    });
    return unwrap(response) ?? [];
}
