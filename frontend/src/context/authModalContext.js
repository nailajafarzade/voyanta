import { createContext, useContext } from "react";

export const AuthModalContext = createContext(null);

/** { openLogin, closeLogin } — giriş pəncərəsini istənilən yerdən açmaq üçün. */
export function useAuthModal() {
  const context = useContext(AuthModalContext);
  if (!context) {
    throw new Error("useAuthModal yalnız <AuthModalProvider> içində istifadə oluna bilər");
  }
  return context;
}
