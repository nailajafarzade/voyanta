/**
 * accessToken və refreshToken-in tək saxlanma yeri.
 *
 * localStorage seçilib ki, səhifə yenilənəndə sessiya bərpa oluna bilsin
 * (AuthContext açılışda buradan oxuyur). Backend refresh token-i rotasiya edir,
 * ona görə `setTokens` hər zaman ikisini birlikdə yazır.
 */
const ACCESS_TOKEN_KEY = "voyanta.accessToken";
const REFRESH_TOKEN_KEY = "voyanta.refreshToken";
function safeRead(key) {
    try {
        return window.localStorage.getItem(key);
    }
    catch {
        // Private mode / bloklanmış storage
        return null;
    }
}
export function getAccessToken() {
    return safeRead(ACCESS_TOKEN_KEY);
}
export function getRefreshToken() {
    return safeRead(REFRESH_TOKEN_KEY);
}
export function hasTokens() {
    return getAccessToken() !== null || getRefreshToken() !== null;
}
export function setTokens(accessToken, refreshToken) {
    try {
        window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
        window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
    catch {
        // storage əlçatan deyilsə token yalnız cari sorğu üçün itir — dağıdıcı deyil
    }
}
export function clearTokens() {
    try {
        window.localStorage.removeItem(ACCESS_TOKEN_KEY);
        window.localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
    catch {
        /* yuxarıdakı kimi */
    }
}
