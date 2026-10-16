import { useEffect, useState } from "react";
import voyanta from "../../assets/Logo Mark.png";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/authContext";
import { useAuthModal } from "../../context/authModalContext";
import { usePlanner } from "../../context/plannerContext";

const NAV_LINKS = [
  { to: "/profil", label: "Profil" },
  { to: "/favorites", label: "Bəyəndiklərim" },
  { to: "/location", label: "İstiqamətlər" },
];

function Header() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const { isAuthenticated, logout } = useAuth();
  const { openLogin } = useAuthModal();
  const { openPlanner } = usePlanner();
  const navigate = useNavigate();

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 10);
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Escape ilə mobil menyunu bağla
  useEffect(() => {
    if (!isMenuOpen) return undefined;

    const onKey = (event) => {
      if (event.key === "Escape") setIsMenuOpen(false);
    };
    document.addEventListener("keydown", onKey);
    // menyu açıqda arxa plan sürüşməsinin qarşısı alınır
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [isMenuOpen]);

  // Səhifə dəyişəndə menyunu bağla
  useEffect(() => {
    setIsMenuOpen(false);
  }, [navigate]);

  const closeMenu = () => setIsMenuOpen(false);

  // Daxil olmayıbsa Google giriş pəncərəsi açılır, daxil olubsa "Çıxış" işləyir
  const handleAuthClick = async () => {
    if (isAuthenticated) {
      await logout();
      navigate("/", { replace: true });
    } else {
      openLogin();
    }
  };

  const handleLogin = () => {
    closeMenu();
    handleAuthClick();
  };

  const handlePlan = () => {
    closeMenu();
    openPlanner();
  };

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 border-b transition-all duration-300 ${
        isScrolled
          ? "border-slate-200/70 bg-white/85 py-2.5 shadow-soft backdrop-blur-xl"
          : "border-transparent bg-white/60 py-4 backdrop-blur-md"
      }`}
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <NavLink
            to="/"
            onClick={closeMenu}
            className="group flex items-center gap-2 rounded-lg p-1"
            aria-label="Voyanta — ana səhifə"
          >
            <img
              className="h-8 w-8 transition-transform duration-300 group-hover:scale-110 sm:h-9 sm:w-9"
              src={voyanta}
              alt=""
            />
            <span className="text-xl font-extrabold tracking-tight text-ink-900 sm:text-2xl">
              voyanta
            </span>
          </NavLink>

          {/* Desktop Navigation */}
          <nav className="hidden items-center gap-1 md:flex">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `rounded-full px-4 py-2 text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? "bg-brand-50 text-brand-700"
                      : "text-ink-600 hover:bg-slate-100 hover:text-ink-900"
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>

          {/* Desktop actions */}
          <div className="hidden items-center gap-3 md:flex">
            <button
              type="button"
              onClick={handleAuthClick}
              className="rounded-full px-4 py-2 text-sm font-medium text-ink-700 transition-all duration-200 hover:bg-slate-100 hover:text-ink-900"
            >
              {isAuthenticated ? "Çıxış" : "Daxil ol"}
            </button>

            <button
              type="button"
              onClick={handlePlan}
              className="group relative overflow-hidden rounded-full bg-ink-900 px-5 py-2.5 text-sm font-semibold text-white shadow-soft transition-all duration-300 hover:bg-ink-800 active:scale-95"
            >
              <span className="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/20 to-transparent transition-transform duration-700 group-hover:translate-x-full" />
              <span className="relative">Plan qur</span>
            </button>
          </div>

          {/* Mobile Menu Button */}
          <button
            type="button"
            onClick={() => setIsMenuOpen((prev) => !prev)}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-ink-700 transition-all duration-200 hover:bg-slate-200 active:scale-95 md:hidden"
            aria-label={isMenuOpen ? "Menyunu bağla" : "Menyunu aç"}
            aria-expanded={isMenuOpen}
          >
            <svg
              className={`h-5 w-5 transition-transform duration-300 ${
                isMenuOpen ? "rotate-90" : ""
              }`}
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.2"
              strokeLinecap="round"
            >
              {isMenuOpen ? (
                <path d="M6 6l12 12M18 6L6 18" />
              ) : (
                <path d="M4 7h16M4 12h16M4 17h10" />
              )}
            </svg>
          </button>
        </div>


        {/* Mobile Navigation */}
        <div
          className={`overflow-hidden transition-all duration-400 md:hidden ${
            isMenuOpen
              ? "mt-4 max-h-96 opacity-100"
              : "max-h-0 opacity-0"
          }`}
        >
          <nav className="flex flex-col gap-1.5 pb-2">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                onClick={closeMenu}
                className={({ isActive }) =>
                  `rounded-2xl px-4 py-3.5 text-sm font-medium transition-colors duration-200 ${
                    isActive
                      ? "bg-brand-50 text-brand-700"
                      : "bg-slate-50 text-ink-700 hover:bg-slate-100"
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}

            <button
              type="button"
              onClick={handlePlan}
              className="mt-1 w-full rounded-2xl bg-ink-900 px-4 py-3.5 text-sm font-semibold text-white transition-colors duration-200 active:scale-[0.98]"
            >
              Plan qur
            </button>

            <button
              type="button"
              onClick={handleLogin}
              className="w-full rounded-2xl bg-slate-50 px-4 py-3.5 text-left text-sm font-medium text-ink-700 transition-colors duration-200 hover:bg-slate-100"
            >
              {isAuthenticated ? "Çıxış" : "Daxil ol"}
            </button>
          </nav>
        </div>
      </div>

      {/* mobil menyu açıq olduqa fon qatı */}
      {isMenuOpen && (
        <button
          type="button"
          onClick={closeMenu}
          aria-hidden="true"
          tabIndex={-1}
          className="fixed inset-0 top-[72px] z-[-1] bg-ink-900/20 backdrop-blur-[2px] md:hidden"
        />
      )}
    </header>
  );
}

export default Header;
