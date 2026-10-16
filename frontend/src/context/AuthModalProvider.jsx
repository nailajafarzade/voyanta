import { useCallback, useMemo, useState } from "react";
import LoginForm from "../components/auth/LoginForm";
import { AuthModalContext } from "./authModalContext";

/**
 * Giriş modalını bir dəfə render edir; Header, plan səhifəsi, qorunan səhifələr və
 * ürək düyməsi onu useAuthModal().openLogin() ilə açır.
 * <AuthProvider> içində olmalıdır (LoginForm useAuth istifadə edir).
 */
function AuthModalProvider({ children }) {
  const [isOpen, setIsOpen] = useState(false);

  const openLogin = useCallback(() => setIsOpen(true), []);
  const closeLogin = useCallback(() => setIsOpen(false), []);

  const value = useMemo(() => ({ openLogin, closeLogin }), [openLogin, closeLogin]);

  return (
    <AuthModalContext.Provider value={value}>
      {children}
      <LoginForm isOpen={isOpen} onClose={closeLogin} />
    </AuthModalContext.Provider>
  );
}

export default AuthModalProvider;
