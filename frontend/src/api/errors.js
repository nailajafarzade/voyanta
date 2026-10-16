import axios from "axios";
/**
 * Backend errorCode → istifadəçiyə göstərilən Azərbaycanca mesaj.
 * Burada olmayan kodlar üçün backend-in öz `error` mətni istifadə olunur.
 */
const ERROR_MESSAGES = {
    VALIDATION_FAILED: "Məlumatlar düzgün doldurulmayıb, yenidən yoxla.",
    INVALID_CREDENTIALS: "E-poçt və ya şifrə yanlışdır.",
    INVALID_OAUTH_TOKEN: "Google/Apple ilə giriş təsdiqlənmədi, yenidən yoxla.",
    INVALID_REFRESH_TOKEN: "Sessiyan etibarsızdır, yenidən daxil ol.",
    REFRESH_TOKEN_EXPIRED: "Sessiyanın vaxtı bitdi, yenidən daxil ol.",
    AUTH_REQUIRED: "Bu əməliyyat üçün hesabına daxil olmalısan.",
    PLAN_ALREADY_CLAIMED: "Bu plan artıq başqa hesaba bağlıdır.",
    RESOURCE_NOT_FOUND: "Axtardığın məlumat tapılmadı.",
    RATE_LIMIT_EXCEEDED: "Gündəlik plan limitinə çatdın, bir az sonra yenidən yoxla.",
    INTERNAL_ERROR: "Gözlənilməz xəta baş verdi, bir az sonra yenidən yoxla.",
    AI_EMPTY_RESPONSE: "Plan hazırlanmadı, zəhmət olmasa yenidən cəhd et.",
    AI_PARSE_ERROR: "Plan hazırlanarkən xəta baş verdi, yenidən cəhd et.",
    // Backend-də mövcud olan, tapşırıq siyahısında olmayan kodlar
    EMAIL_TAKEN: "Bu e-poçt artıq qeydiyyatdan keçib.",
    SURVEY_INCOMPLETE: "Sorğu tam doldurulmayıb — bütün addımları tamamla.",
    UNKNOWN_PROVIDER: "Bu giriş üsulu dəstəklənmir.",
    USER_NOT_FOUND: "İstifadəçi tapılmadı.",
    // Yalnız frontend-in özü yaratdığı kodlar
    NETWORK_ERROR: "Serverə qoşulmaq mümkün olmadı. Backend işləyirmi?",
    EMAIL_AUTH_DISABLED: "E-poçt/şifrə ilə giriş serverdə hazırda aktiv deyil. Zəhmət olmasa Google ilə davam et.",
    OAUTH_NOT_CONFIGURED: "Google ilə giriş hələ konfiqurasiya edilməyib (VITE_GOOGLE_CLIENT_ID boşdur).",
};
const FALLBACK_MESSAGE = "Xəta baş verdi, bir az sonra yenidən yoxla.";
/** Verilmiş kod üçün UI mesajı (yoxdursa backend mətni, o da yoxdursa fallback). */
export function messageForCode(errorCode, backendMessage) {
    if (errorCode && ERROR_MESSAGES[errorCode]) {
        return ERROR_MESSAGES[errorCode];
    }
    return backendMessage || FALLBACK_MESSAGE;
}
/** Frontend-in özünün yaratdığı xətalar üçün. */
export function createApiError(errorCode, status = null) {
    return { message: messageForCode(errorCode), errorCode, status };
}
/** İstənilən `catch (error)` dəyərini vahid ApiError formasına çevirir. */
export function toApiError(error) {
    if (axios.isAxiosError(error)) {
        if (!error.response) {
            return createApiError("NETWORK_ERROR");
        }
        const status = error.response.status;
        const body = error.response.data;
        const errorCode = body?.errorCode ?? null;
        // VALIDATION_FAILED cavabında sahə xətaları `data` içindədir
        const fieldErrors = errorCode === "VALIDATION_FAILED" && body?.data && typeof body.data === "object"
            ? body.data
            : undefined;
        return {
            message: messageForCode(errorCode, body?.error),
            errorCode,
            status,
            fieldErrors,
        };
    }
    if (error && typeof error === "object" && "errorCode" in error && "message" in error) {
        return error;
    }
    return { message: FALLBACK_MESSAGE, errorCode: null, status: null };
}
/** Qısa yol: birbaşa göstərilə bilən mesaj. */
export function apiErrorMessage(error) {
    const apiError = toApiError(error);
    if (apiError.fieldErrors) {
        const first = Object.values(apiError.fieldErrors)[0];
        if (first)
            return first;
    }
    return apiError.message;
}
