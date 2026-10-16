import { Link } from "react-router-dom";
import voyanta from "../../assets/Logo Mark.png";
import Reveal from "../common/Reveal";
import { usePlanner } from "../../context/plannerContext";

const FOOTER_LINKS = [
  { label: "Profil", to: "/profil" },
  { label: "Bəyəndiklərim", to: "/favorites" },
  { label: "İstiqamətlər", to: "/location" },
];

const FOOTER_LINKS_TEXT = [
  { label: "Əlaqə", href: "#" },
  { label: "Məxfilik siyasəti", href: "#" },
  { label: "İstifadə şərtləri", href: "#" },
];

function Footer() {
  const { openPlanner } = usePlanner();

  return (
    <footer className="w-full font-sans">
      {/* ============ CTA BLOKU ============ */}
      <div className="relative overflow-hidden bg-ink-900 px-4 py-20 sm:px-6 lg:px-8 lg:py-24">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -left-20 -top-20 h-80 w-80 rounded-full bg-brand-600/25 blur-3xl animate-voy-drift"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -bottom-24 -right-16 h-80 w-80 rounded-full bg-mint-500/20 blur-3xl animate-voy-float-slow"
        />

        <div className="relative mx-auto flex max-w-4xl flex-col items-center justify-center text-center">
          <Reveal>
            <span className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-[11px] font-bold uppercase tracking-[0.12em] text-brand-200">
              <span aria-hidden="true">✈️</span>
              pulsuz başla
            </span>
          </Reveal>

          <Reveal delay={80}>
            <h2 className="mt-6 text-3xl font-extrabold leading-[1.12] tracking-tight text-white sm:text-4xl lg:text-5xl">
              Növbəti səyahətin bir neçə sual{" "}
              <span className="bg-gradient-to-r from-brand-300 to-mint-300 bg-clip-text text-transparent">
                uzaqlığındadır
              </span>
            </h2>
          </Reveal>

          <Reveal delay={160}>
            <p className="mt-5 max-w-lg text-sm leading-relaxed text-white/55 sm:text-base">
              7 sual ver, AI sənə uyğun gündəlik marşrut hazırlasın. Pulsuz,
              qeydiyyatsız.
            </p>
          </Reveal>

          <Reveal delay={240}>
            <button
              type="button"
              onClick={openPlanner}
              className="group relative mt-9 inline-flex items-center gap-2 overflow-hidden rounded-full bg-white px-8 py-4 text-base font-bold text-ink-900 shadow-glow transition-all duration-300 hover:bg-brand-50 active:scale-95"
            >
              <span className="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-brand-100 to-transparent transition-transform duration-700 group-hover:translate-x-full" />
              <span className="relative">İndi planlaşdır</span>
              <span
                className="relative transition-transform duration-300 group-hover:translate-x-1"
                aria-hidden="true"
              >
                →
              </span>
            </button>
          </Reveal>
        </div>
      </div>


      {/* ============ ALT BLOK ============ */}
      <div className="border-t border-slate-200/60 bg-canvas px-4 py-12 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="flex flex-col gap-10 md:flex-row md:items-start md:justify-between">
            {/* Logo + təsvir */}
            <div className="max-w-sm">
              <Link
                to="/"
                className="inline-flex items-center gap-2"
                aria-label="Voyanta — ana səhifə"
              >
                <img src={voyanta} alt="" className="h-8 w-8" />
                <span className="text-xl font-extrabold tracking-tight text-ink-900">
                  voyanta
                </span>
              </Link>

              <p className="mt-4 text-sm leading-relaxed text-ink-500">
                Süni intellektlə fərdi səyahət planlaması. Bir neçə sualla öz
                səyahətini qur.
              </p>
            </div>

            {/* Linklər */}
            <div className=" gap-10 sm:gap-16">
              <div>
                <h3 className="text-[11px] font-bold uppercase tracking-[0.12em] text-ink-400">
                  Keçid
                </h3>
                <ul className="mt-4 space-y-2.5">
                  {FOOTER_LINKS.map((link) => (
                    <li key={link.to}>
                      <Link
                        to={link.to}
                        className="text-sm text-ink-600 transition-colors duration-200 hover:text-brand-600"
                      >
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>


            </div>
          </div>

          <div className="mt-10 flex flex-col items-center justify-between gap-4 border-t border-slate-200/70 pt-7 sm:flex-row">
            <p className="text-center text-xs text-ink-400 sm:text-left">
              © Voyanta 2026. Bütün hüquqlar qorunur.
            </p>

            <p className="flex items-center gap-1.5 text-xs text-ink-400">
              <span className="relative flex h-1.5 w-1.5">
                <span className="absolute inline-flex h-full w-full rounded-full bg-mint-500 opacity-60 animate-voy-pulse-ring" />
                <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-mint-500" />
              </span>
              AI ilə hazırlanır
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
