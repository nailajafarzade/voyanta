import { useEffect, useRef, useState } from "react";

export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

let scriptLoadPromise = null;

/**
 * index.html-də <script src="https://accounts.google.com/gsi/client"> artıq var,
 * async defer ilə yükləndiyi üçün hazır olana qədər gözləyir.
 */
function waitForGoogleScript() {
    if (window.google?.accounts?.id) return Promise.resolve();
    if (scriptLoadPromise) return scriptLoadPromise;

    scriptLoadPromise = new Promise((resolve, reject) => {
        const start = Date.now();
        const check = () => {
            if (window.google?.accounts?.id) { resolve(); return; }
            if (Date.now() - start > 10_000) {
                reject(new Error("Google Identity Services skripti yüklənmədi"));
                return;
            }
            window.setTimeout(check, 100);
        };
        check();
    });
    return scriptLoadPromise;
}

/**
 * Google Sign-In hook-u.
 *
 * renderButton yanaşması — hidden div + click ötürmə yox.
 * Google öz button-unu birbaşa `buttonRef` div-inə render edir.
 * Callback-dən ID token (JWT) gəlir → onIdToken çağırılır → backend-ə göndərilir.
 *
 * onIdToken null olarsa initialize edilmir (modal bağlı olduqda).
 */
export function useGoogleSignIn(onIdToken) {
    const buttonRef = useRef(null);
    const [isReady, setIsReady] = useState(false);
    const [loadError, setLoadError] = useState(null);
    const onIdTokenRef = useRef(onIdToken);
    onIdTokenRef.current = onIdToken;

    useEffect(() => {
        if (!GOOGLE_CLIENT_ID || !onIdToken) return;
        let cancelled = false;

        waitForGoogleScript()
            .then(() => {
                if (cancelled || !buttonRef.current) return;

                window.google.accounts.id.initialize({
                    client_id: GOOGLE_CLIENT_ID,
                    callback: (response) => {
                        // response.credential — ID token (JWT)
                        // Backend: POST /api/auth/oauth/google { idToken: ... }
                        if (onIdTokenRef.current) {
                            onIdTokenRef.current(response.credential);
                        }
                    },
                    use_fedcm_for_prompt: true,
                });

                window.google.accounts.id.renderButton(buttonRef.current, {
                    type: "standard",
                    theme: "outline",
                    size: "large",
                    shape: "rectangular",
                    width: buttonRef.current.offsetWidth || 360,
                    logo_alignment: "left",
                });

                setIsReady(true);
            })
            .catch((err) => {
                if (!cancelled) setLoadError(err.message);
            });

        return () => {
            cancelled = true;
            window.google?.accounts?.id?.cancel();
        };
    }, [onIdToken]); // onIdToken dəyişəndə (null → fn) yenidən initialize edir

    return { buttonRef, isReady, loadError };
}