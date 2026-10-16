import { createContext, useContext } from "react";

/**
 * Sorğu pəncərəsini (7 addımlı plan) istənilən yerdən açmaq üçün.
 * Hero, Header və Footer eyni pəncərəni paylaşır.
 */
export const PlannerContext = createContext(null);

export function usePlanner() {
  return useContext(PlannerContext);
}
