import { useEffect, useMemo, useState } from "react";
import { getFeaturedDestinations, getPopularDestinations } from "../api/destinations";
import { getPersonalizedDestinations } from "../api/recommendations";
import { useAuth } from "../context/authContext";
import { toPlace } from "../utils/destinations";

/**
 * Static homepage cards (Popular / Seasonal) use local slug IDs, while the
 * live backend (`GET/POST/DELETE /api/wishlist/{destinationId}`) only accepts
 * real destination UUIDs. This hook loads the existing backend destination
 * lists (featured + popular + personalized) ONCE and resolves a static display
 * name to the real backend `place` object — frontend-only, no backend change.
 *
 * When a static place has no backend row (e.g. Dolomites, Lapland), the
 * resolver returns `null` and the caller renders a visible-but-disabled heart
 * instead of inventing an ID.
 */

/** Case/accent/whitespace-insensitive name key. */
export function normalisePlaceName(value) {
  return String(value ?? "")
    .toLowerCase()
    .replace(/ı/g, "i")
    .replace(/ə/g, "e")
    .replace(/ö/g, "o")
    .replace(/ü/g, "u")
    .replace(/ş/g, "s")
    .replace(/ç/g, "c")
    .replace(/ğ/g, "g")
    .replace(/[^a-z0-9]+/g, "")
    .trim();
}

/** Static card label -> backend catalogue spelling(s) (keys/values are normalised). */
const NAME_ALIASES = {
  bangkok: ["banqkok"],
  seoul: ["seul"],
  prague: ["praqa"],
  vienna: ["vyana"],
  // Card label != catalogue spelling (e.g. "Kioto" / "Kyoto"). Without the
  // alias the heart gets no `id` and the click silently does nothing.
  kioto: ["kyoto"],
  barselona: ["barcelona"],
  nyuyork: ["newyork"],
  amalfisahili: ["amalfi"],
  zenzibar: ["zanzibar"],
  merrakes: ["marrakes"],
};

let cache = null; // { places, at }
let inflight = null;
const MAX_AGE_MS = 5 * 60 * 1000;

function readCache() {
  if (!cache) return null;
  if (Date.now() - cache.at > MAX_AGE_MS) {
    cache = null;
    return null;
  }
  return cache.places;
}

function loadAll(includePersonalized) {
  if (inflight) return inflight;
  inflight = (async () => {
    const settled = await Promise.allSettled([
      getFeaturedDestinations(),
      // The resolver index must hold EVERY catalogue row (57+), not just the
      // homepage window of 50 — otherwise e.g. "Zanzibar" never resolves and
      // its heart gets no `id`.
      getPopularDestinations(500),
      includePersonalized ? getPersonalizedDestinations(50) : Promise.resolve([]),
    ]);
    const seen = new Map();
    for (const result of settled) {
      if (result.status !== "fulfilled") continue;
      for (const destination of result.value ?? []) {
        const place = toPlace(destination);
        if (place?.id && !seen.has(place.id)) seen.set(place.id, place);
      }
    }
    const places = [...seen.values()];
    cache = { places, at: Date.now() };
    return places;
  })().finally(() => {
    inflight = null;
  });
  return inflight;
}

export function useBackendPlaces() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const [places, setPlaces] = useState(() => readCache() ?? []);
  const [isLoading, setIsLoading] = useState(() => !readCache());

  useEffect(() => {
    if (authLoading) return undefined;
    const cached = readCache();
    if (cached) {
      setPlaces(cached);
      setIsLoading(false);
      return undefined;
    }
    let cancelled = false;
    setIsLoading(true);
    loadAll(isAuthenticated)
      .then((loaded) => {
        if (!cancelled) {
          setPlaces(loaded);
          setIsLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [authLoading, isAuthenticated]);

  const index = useMemo(() => {
    const map = new Map();
    for (const place of places) {
      const key = normalisePlaceName(place.title?.split(",")[0]);
      if (key && !map.has(key)) map.set(key, place);
    }
    return map;
  }, [places]);

  const resolveBackendPlace = (name) => {
    const norm = normalisePlaceName(name);
    if (!norm) return null;
    if (index.has(norm)) return index.get(norm);
    for (const alt of NAME_ALIASES[norm] ?? []) {
      if (index.has(alt)) return index.get(alt);
    }
    return null;
  };

  return { places, isLoading, resolveBackendPlace };
}

export default useBackendPlaces;
