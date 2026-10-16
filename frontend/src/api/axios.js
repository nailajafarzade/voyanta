import axios from "axios";
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from "./tokenStorage";
import { createApiError, messageForCode } from "./errors";
const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";
const api = axios.create({
    baseURL,
    headers: {
        "Content-Type": "application/json",
    },
});
/**
 * Refresh sorğusu üçün ayrı instance — aşağıdaki interceptor-lar ona toxunmur,
 * beləliklə refresh özü 401 alsa sonsuz dövrə yaranmır.
 */
const refreshClient = axios.create({
    baseURL,
    headers: {
        "Content-Type": "application/json",
    },
});
/** Refresh alınmayanda yayımlanır — AuthContext bunu dinləyib state-i təmizləyir. */
export const AUTH_EXPIRED_EVENT = "voyanta:auth-expired";
/** Bu yollarda 401 gələndə refresh cəhdi etmirik (özləri auth axınının hissəsidir). */
const NO_REFRESH_PATHS = ["/auth/refresh", "/auth/login", "/auth/register", "/auth/oauth/"];
/**
 * Eyni anda bir neçə sorğu 401 alanda yalnız BİR refresh sorğusu gedir —
 * qalanları həmin promise-i gözləyir (backend token-i rotasiya etdiyi üçün
 * paralel refresh ikinci sorğunu etibarsız edərdi).
 */
let refreshPromise = null;
async function requestNewTokens() {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
        throw createApiError("INVALID_REFRESH_TOKEN");
    }
    const response = await refreshClient.post("/auth/refresh", {
        refreshToken,
    });
    const data = response.data?.data;
    if (!response.data?.success || !data?.accessToken || !data?.refreshToken) {
        throw createApiError("INVALID_REFRESH_TOKEN");
    }
    // Rotasiya: backend hər refresh-də yeni cüt verir, ikisi də əvəz olunmalıdır
    setTokens(data.accessToken, data.refreshToken);
    return data.accessToken;
}
function refreshTokens() {
    if (!refreshPromise) {
        refreshPromise = requestNewTokens().finally(() => {
            refreshPromise = null;
        });
    }
    return refreshPromise;
}
api.interceptors.request.use((config) => {
    const token = getAccessToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
api.interceptors.response.use((response) => response, async (error) => {
    if (!axios.isAxiosError(error)) {
        return Promise.reject(error);
    }
    const config = error.config;
    const url = config?.url ?? "";
    const shouldTryRefresh = error.response?.status === 401 &&
        config !== undefined &&
        config._voyantaRetried !== true &&
        getRefreshToken() !== null &&
        !NO_REFRESH_PATHS.some((path) => url.includes(path));
    if (!shouldTryRefresh) {
        return Promise.reject(error);
    }
    // Yalnız bir dəfə təkrar — sonsuz dövrənin qarşısını alır
    config._voyantaRetried = true;
    try {
        const accessToken = await refreshTokens();
        config.headers.Authorization = `Bearer ${accessToken}`;
        return await api.request(config);
    }
    catch {
        clearTokens();
        window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
        return Promise.reject(error);
    }
});
/**
 * { success, data, error, errorCode } zərfindən `data`-nı çıxarır.
 * success = false olan 2xx cavabı gəlsə də burada xətaya çevrilir.
 */
export function unwrap(response) {
    const body = response.data;
    if (!body || body.success !== true) {
        const errorCode = body?.errorCode ?? "INTERNAL_ERROR";
        const apiError = {
            errorCode,
            status: response.status,
            message: messageForCode(errorCode, body?.error),
        };
        throw apiError;
    }
    return body.data;
}
export default api;
