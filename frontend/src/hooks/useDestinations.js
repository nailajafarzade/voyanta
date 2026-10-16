import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getFeaturedDestinations,
  getPopularDestinations,
} from "../api/destinations";
import { getPersonalizedDestinations } from "../api/recommendations";
import { apiErrorMessage, toApiError } from "../api/errors";
import { toPlace } from "../utils/destinations";
import { createImagePlan } from "../utils/imageAssignment";

/**
 * Ana səhifədə bölmələr eyni siyahını göstərir.
 * Əvvəllər hər biri ayrı-ayrı sorğu göndərirdi — indi modul səviyyəsində
 * BİR sorğu göndərilir və nəticə bütün səhifə üçün paylaşılır.
 *
 * Yeni endpoint yoxdur: eyni `GET /destinations/featured` və
 * `GET /recommendations/personalized` çağırılır.
 *
 * `requireAuth: true` olanda bölmə YALNIZ daxil olmuş istifadəçiyə aiddir
 * ("Sənin üçün seçilmiş yerlər"): gonaq üçün sorğu göndərilmir, `authRequired`
 * qaytarılır və backend-in 401/403 cavabı da eyni vəziyyəti yaradır.
 *
 * Şəkillər hər halda backend-dən gəlir (`imageUrl`) — burada heç bir
 * istiqamət → şəkil seçimi yoxdur.
 */
const cache = new Map(); // key -> { places, at }
const inFlight = new Map(); // key -> Promise (eşzamanlı sorğuları birləşdirir)

const MAX_AGE_MS = 5 * 60 * 1000; // 5 dəqiqə

const LOADERS = {
  featured: (limit) => getFeaturedDestinations(limit),
  popular: (limit, exclude) => getPopularDestinations(limit, exclude),
  personalized: (limit) => getPersonalizedDestinations(limit),
};

/** Backend 401/403 (yaxud AUTH_REQUIRED) qaytardısa = bu bölmə yalnız daxil olmuşlar üçündür. */
function isAuthRequiredError(error) {
  const { status, errorCode } = toApiError(error);
  return status === 401 || status === 403 || errorCode === "AUTH_REQUIRED";
}

function readCache(key) {
  const entry = cache.get(key);
  if (!entry) return null;
  if (Date.now() - entry.at > MAX_AGE_MS) {
    cache.delete(key);
    return null;
  }
  return entry;
}

/**
 * Eyni anda bir neçə komponent (tövsiyələr / trend / mövsüm / istiqamətlər)
 * bu hook-u çağırsın deyə, gözləyən sorğu `inFlight` xəritəsində saxlanılır.
 * Beləliklə səhifə yükləndikdə BİR şəbəkə sorğusu göndərilir.
 */
function load(key, limit, exclude) {
  const existing = inFlight.get(key);
  if (existing) return existing;

  const loader = LOADERS[key] ?? LOADERS.featured;

  const request = (async () => {
    const destinations = await loader(limit, exclude);
    const places = (destinations ?? []).map(toPlace);
    cache.set(key, { places, at: Date.now() });
    return places;
  })().finally(() => {
    inFlight.delete(key);
  });

  inFlight.set(key, request);
  return request;
}

/**
 * @param {{ isAuthenticated: boolean, isAuthLoading?: boolean, limit?: number,
 *           requireAuth?: boolean, source?: 'featured'|'popular'|'personalized',
 *           exclude?: string[] }} params
 */
export function useDestinations({
  isAuthenticated,
  isAuthLoading = false,
  limit,
  requireAuth = false,
  source,
  exclude,
}) {
  // requireAuth = yalnız "Sənin üçün seçilmiş yerlər": gonaq üçün featured-ə düşmür,
  // çünki bu bölmə şəxsi tövsiyədir — göstərilməməlidir.
  const key = source ?? (requireAuth || isAuthenticated ? "personalized" : "featured");
  const needsAuth = key === "personalized";

  const initial = readCache(key);

  const [state, setState] = useState(() => ({
    places: initial?.places ?? [],
    isLoading: !initial,
    error: null,
    authRequired: false,
  }));

  // "Yenidən cəhd et" üçün effect-i yenidən başlatma qaldırıcısı
  const [retryKey, setRetryKey] = useState(0);

  // `exclude` backend-ə göndərilir; stabil serialize edilir ki, hər render-da
  // yeni array yaradıb effect-i boş yerdən tetikləməsin.
  const excludeKey = useMemo(
    () => (Array.isArray(exclude) && exclude.length ? [...exclude].sort().join(",") : ""),
    [exclude]
  );

  useEffect(() => {
    if (isAuthLoading) return undefined;

    // Gonaq: sorğu göndərilmir, dərhal qeydiyyat promptu göstərilir
    if (needsAuth && !isAuthenticated) {
      setState({ places: [], isLoading: false, error: null, authRequired: true });
      return undefined;
    }

    const cached = readCache(key);
    if (cached) {
      setState({ places: cached.places, isLoading: false, error: null, authRequired: false });
      return undefined;
    }

    let cancelled = false;
    setState((prev) => ({ ...prev, isLoading: true, error: null, authRequired: false }));

    (async () => {
      try {
        const places = await load(
          key,
          limit,
          excludeKey ? excludeKey.split(",") : undefined
        );

        if (!cancelled) {
          setState({ places, isLoading: false, error: null, authRequired: false });
        }
      } catch (caught) {
        if (!cancelled) {
          // Sessiya bitib (401/403) → "qeydiyyatdan keç" promptu, xəta yox
          if (isAuthRequiredError(caught)) {
            setState({ places: [], isLoading: false, error: null, authRequired: true });
            return;
          }
          setState({ places: [], isLoading: false, error: apiErrorMessage(caught), authRequired: false });
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [key, limit, excludeKey, isAuthLoading, isAuthenticated, needsAuth, retryKey]);

  const places =
    typeof limit === "number" ? state.places.slice(0, limit) : state.places;

  // "Yenidən cəhd et" — yaddaşı təmizləyib sorğunu təkrar edir
  const reload = useCallback(() => {
    cache.delete(key);
    setRetryKey((n) => n + 1);
  }, [key]);

  return {
    places,
    allPlaces: state.places,
    isLoading: state.isLoading,
    error: state.error,
    authRequired: state.authRequired,
    reload,
  };
}

export default useDestinations;

/**
 * Bütün səhifə bölmələri üçün ŞƏKİL PLANI.
 *
 * Plan `useMemo` ilə hesablanır → React yenidən render etsə də EYNİ nəticə
 * qaytarılır (təsadüfi seçim YOXDUR, `Math.random()` istifadə olunmur).
 * Plan heç bir istiqamət obyektini dəyişmir, yalnız oxumaq üçün bir Map-dir.
 *
 * @param {Array<{ key: string, places?: Array }>} sections Ekran sırası ilə
 */
export function useImagePlan(sections) {
  return useMemo(() => createImagePlan(sections), [sections]);
}
