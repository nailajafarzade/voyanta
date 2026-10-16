import { useEffect, useRef, useState } from "react";
import voyanta from "../../assets/Logo Mark.png";
import { useAuth } from "../../context/authContext";
import { apiErrorMessage, messageForCode } from "../../api/errors";
import { GOOGLE_CLIENT_ID, useGoogleSignIn } from "../../hooks/useGoogleSignIn";

function LoginForm({ isOpen, onClose }) {
    const [shouldRender, setShouldRender] = useState(false);
    const [animate, setAnimate] = useState(false);
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const { loginWithOAuth } = useAuth();

    // Google button container ref — renderButton bu div-ə render edir.
    // Modal açılanda ref hazır olsun deyə shouldRender true olandan sonra
    // useGoogleSignIn effect-i yenidən işə düşür (key prop ilə).
    const googleContainerRef = useRef(null);

    useEffect(() => {
        if (isOpen) {
            setShouldRender(true);
            setError(null);
            const timer = setTimeout(() => setAnimate(true), 10);
            return () => clearTimeout(timer);
        } else {
            setAnimate(false);
            const timer = setTimeout(() => setShouldRender(false), 200);
            return () => clearTimeout(timer);
        }
    }, [isOpen]);

    // Google-dan gələn ID token backend-ə göndərilir: POST /api/auth/oauth/google.
    // Backend-də email/şifrə söndürülüb — giriş və qeydiyyat YALNIZ Google ilədir,
    // ilk dəfə daxil olan hesab avtomatik yaranır.
    const handleIdToken = async (idToken) => {
        setError(null);
        setIsSubmitting(true);
        try {
            await loginWithOAuth("google", idToken);
            onClose();
        } catch (caught) {
            setError(apiErrorMessage(caught));
        } finally {
            setIsSubmitting(false);
        }
    };

    const { buttonRef, loadError } = useGoogleSignIn(
        shouldRender ? handleIdToken : null,
        googleContainerRef
    );

    return (
        <>
            {shouldRender && (
                <div
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="login-title"
                    className={`fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-md transition-opacity duration-300 ${
                        animate ? "bg-ink-900/60 opacity-100" : "bg-ink-900/0 opacity-0"
                    }`}
                    onClick={onClose}
                >
                    <div
                        onClick={(e) => e.stopPropagation()}
                        className={`relative w-full max-w-md overflow-hidden rounded-4xl border border-slate-100 bg-white p-8 shadow-lift sm:p-10 ${
                            animate
                                ? "translate-y-0 scale-100 opacity-100"
                                : "translate-y-8 scale-95 opacity-0"
                        } transition-all duration-400 ease-out`}
                    >
                        {/* dekorativ üst qat */}
                        <div
                            aria-hidden="true"
                            className="pointer-events-none absolute inset-x-0 top-0 h-32 bg-gradient-to-b from-brand-50 to-transparent"
                        />

                        <button
                            type="button"
                            onClick={onClose}
                            aria-label="Bağla"
                            className="absolute right-5 top-5 z-10 rounded-full p-2 text-ink-400 transition-all duration-200 hover:bg-slate-100 hover:text-ink-700 active:scale-90"
                        >
                            <svg
                                className="h-5 w-5"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth="2.2"
                                    d="M6 18L18 6M6 6l12 12"
                                />
                            </svg>
                        </button>

                        <div className="relative flex flex-col items-center text-center">
                            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-white shadow-soft ring-1 ring-slate-100">
                                <img src={voyanta} alt="" className="h-9 w-9" />
                            </div>

                            <h2
                                id="login-title"
                                className="text-2xl font-extrabold tracking-tight text-ink-900 sm:text-3xl"
                            >
                                Hesab yarat
                            </h2>

                            <p className="mt-2 max-w-xs text-sm leading-relaxed text-ink-500">
                                Planlarını yadda saxlamaq üçün pulsuz qeydiyyatdan keç
                            </p>
                        </div>

                        {(error || loadError) && (
                            <p
                                role="alert"
                                className="mt-6 w-full rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-xs leading-relaxed text-red-600"
                            >
                                {error || loadError}
                            </p>
                        )}

                        {!GOOGLE_CLIENT_ID ? (
                            <p className="mt-7 text-[11px] leading-relaxed text-amber-600">
                                Google ilə giriş hələ konfiqurasiya edilməyib (VITE_GOOGLE_CLIENT_ID
                                boşdur).
                            </p>
                        ) : (
                            <div className="mt-7">
                                {isSubmitting && (
                                    <div className="mb-3 flex items-center justify-center gap-2 text-sm text-ink-500">
                                        <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-ink-700" />
                                        <span>Daxil olunur…</span>
                                    </div>
                                )}
                                {/* Google öz button-unu bu div-ə render edir.
                    overflow-hidden + flex center ilə Google-un button-u
                    container-ə uyğun mərkəzlənir. */}
                                <div
                                    ref={buttonRef}
                                    className="flex w-full justify-center overflow-hidden"
                                    style={{ minHeight: 44 }}
                                />
                            </div>
                        )}

                        <p className="mt-7 max-w-xs text-[11px] leading-relaxed text-ink-400">
                            Davam etməklə{" "}
                            <a href="#" className="underline hover:text-ink-600">
                                İstifadə şərtləri
                            </a>{" "}
                            və{" "}
                            <a href="#" className="underline hover:text-ink-600">
                                Məxfilik siyasətini
                            </a>{" "}
                            qəbul edirsən.
                        </p>
                    </div>
                </div>
            )}
        </>
    );
}

export default LoginForm;