import { Outlet } from "react-router-dom";
import { useAuth } from "../../context/authContext";
import { useAuthModal } from "../../context/authModalContext";

/**
 * /profil və /favorites yalnız daxil olmuş istifadəçi üçündür.
 * Token bərpa olunarkən yönləndirmə etmir — əks halda səhifə yenilənəndə
 * daxil olmuş istifadəçi də giriş pəncərəsi görərdi.
 */
function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  const { openLogin } = useAuthModal();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas">
        <div className="flex flex-col items-center gap-4">
          <span className="h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-brand-500" />
          <p className="text-sm text-ink-500">Yüklənir…</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas px-4">
        <div className="w-full max-w-sm rounded-4xl border border-slate-100 bg-white p-8 text-center shadow-soft">
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-brand-50 text-3xl">
            🔒
          </div>

          <h1 className="text-xl font-extrabold tracking-tight text-ink-900">
            Bu səhifə üçün daxil ol
          </h1>

          <p className="mt-2 text-sm leading-relaxed text-ink-500">
            Profilini və bəyəndiyin yerləri görmək üçün Google hesabınla daxil
            ol.
          </p>

          <button
            type="button"
            onClick={openLogin}
            className="mt-7 w-full rounded-full bg-brand-500 px-7 py-3.5 text-sm font-semibold text-white shadow-glow transition-all duration-300 hover:bg-brand-600 active:scale-95"
          >
            Daxil ol
          </button>
        </div>
      </div>
    );
  }

  return <Outlet />;
}

export default ProtectedRoute;
