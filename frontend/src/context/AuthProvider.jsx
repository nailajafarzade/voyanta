import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AUTH_EXPIRED_EVENT } from "../api/axios";
import * as authApi from "../api/auth";
import { getMe } from "../api/user";
import { clearTokens, hasTokens } from "../api/tokenStorage";
import { AuthContext } from "./authContext";
function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [isLoading, setIsLoading] = useState(hasTokens());
    const isMounted = useRef(true);
    useEffect(() => {
        isMounted.current = true;
        return () => {
            isMounted.current = false;
        };
    }, []);
    /** Açılışda: saxlanmış token varsa /users/me ilə istifadəçini bərpa et. */
    useEffect(() => {
        if (!hasTokens()) {
            setIsLoading(false);
            return;
        }
        let cancelled = false;
        (async () => {
            try {
                const profile = await getMe();
                if (!cancelled)
                    setUser(profile);
            }
            catch {
                // Token etibarsız/vaxtı bitmiş və refresh də alınmayıb — state-i təmizlə
                clearTokens();
                if (!cancelled)
                    setUser(null);
            }
            finally {
                if (!cancelled)
                    setIsLoading(false);
            }
        })();
        return () => {
            cancelled = true;
        };
    }, []);
    /** Interceptor refresh-i bacarmayanda bu event gəlir. */
    useEffect(() => {
        const handleExpired = () => {
            setUser(null);
            setIsLoading(false);
        };
        window.addEventListener(AUTH_EXPIRED_EVENT, handleExpired);
        return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpired);
    }, []);
    const loadProfile = useCallback(async () => {
        const profile = await getMe();
        if (isMounted.current)
            setUser(profile);
        return profile;
    }, []);
    const login = useCallback(async (email, password) => {
        await authApi.login({ email, password });
        return loadProfile();
    }, [loadProfile]);
    const register = useCallback(async (payload) => {
        await authApi.register(payload);
        return loadProfile();
    }, [loadProfile]);
    const loginWithOAuth = useCallback(async (provider, idToken) => {
        await authApi.oauthLogin(provider, idToken);
        return loadProfile();
    }, [loadProfile]);
    const logout = useCallback(async () => {
        try {
            await authApi.logout();
        }
        catch {
            // Server cavab verməsə də lokal sessiya bağlanmalıdır (token-lər authApi-də silinir)
        }
        finally {
            if (isMounted.current)
                setUser(null);
        }
    }, []);
    const refreshUser = useCallback(async () => {
        if (!hasTokens()) {
            setUser(null);
            return;
        }
        try {
            await loadProfile();
        }
        catch {
            if (isMounted.current)
                setUser(null);
        }
    }, [loadProfile]);
    const value = useMemo(() => ({
        user,
        isAuthenticated: user !== null,
        isLoading,
        login,
        register,
        loginWithOAuth,
        logout,
        refreshUser,
        setUser,
    }), [user, isLoading, login, register, loginWithOAuth, logout, refreshUser]);
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export default AuthProvider;
