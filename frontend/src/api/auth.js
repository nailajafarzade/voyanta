import axios from "axios";
import api, { unwrap } from "./axios";
import { clearTokens, setTokens } from "./tokenStorage";
import { createApiError } from "./errors";
/**
 * DİQQƏT: backend-də (AuthController.java) `/auth/register` və `/auth/login`
 * endpoint-ləri hazırda kommentdədir — yalnız OAuth, refresh və logout aktivdir.
 * Sorğular sənədləşdirilmiş müqaviləyə uyğun göndərilir; server 404/405 qaytardıqda
 * istifadəçiyə saxta uğur göstərmək yerinə aydın mesaj verilir.
 */
function mapMissingEmailAuth(error) {
    if (axios.isAxiosError(error) && (error.response?.status === 404 || error.response?.status === 405)) {
        return createApiError("EMAIL_AUTH_DISABLED", error.response.status);
    }
    return error;
}
export async function register(payload) {
    try {
        const auth = unwrap(await api.post("/auth/register", payload));
        setTokens(auth.accessToken, auth.refreshToken);
        return auth;
    }
    catch (error) {
        throw mapMissingEmailAuth(error);
    }
}
export async function login(payload) {
    try {
        const auth = unwrap(await api.post("/auth/login", payload));
        setTokens(auth.accessToken, auth.refreshToken);
        return auth;
    }
    catch (error) {
        throw mapMissingEmailAuth(error);
    }
}
/**
 * POST /api/auth/oauth/{provider} — `idToken` REAL Google/Apple axınından gəlməlidir.
 * Frontend-də hazır OAuth provider axını olmadığı üçün bu funksiya hələ heç bir
 * UI düyməsindən çağırılmır (bax: LoginForm-daki konfiqurasiya xəbərdarlığı).
 */
export async function oauthLogin(provider, idToken) {
    const auth = unwrap(await api.post(`/auth/oauth/${provider}`, { idToken }));
    setTokens(auth.accessToken, auth.refreshToken);
    return auth;
}
/**
 * Manual refresh. Adi hallarda axios interceptor-u (src/api/axios.ts) bunu
 * avtomatik edir — bu funksiya yalnız açıq çağırış lazım olanda istifadə olunur.
 */
export async function refresh(refreshToken) {
    const auth = unwrap(await api.post("/auth/refresh", { refreshToken }));
    setTokens(auth.accessToken, auth.refreshToken);
    return auth;
}
/** POST /api/auth/logout — boş body, auth tələb olunur. Token-lər hər halda silinir. */
export async function logout() {
    try {
        await api.post("/auth/logout");
    }
    finally {
        clearTokens();
    }
}
