import { useCallback, useMemo, useState } from "react";
import PlanningPage from "../features/planning/PlanningPage";
import { PlannerContext } from "./plannerContext";

/**
 * 7 addımlı sorğunun açıq/bağlı vəziyyətini saxlayır və altında
 * <PlanningPage> render edir. Header, Hero, Footer və səhifələr
 * `usePlanner().openPlanner()` ilə onu açır.
 */
function PlannerProvider({ children }) {
  const [isOpen, setIsOpen] = useState(false);

  const openPlanner = useCallback(() => setIsOpen(true), []);
  const closePlanner = useCallback(() => setIsOpen(false), []);

  const value = useMemo(
    () => ({ isOpen, openPlanner, closePlanner }),
    [isOpen, openPlanner, closePlanner]
  );

  return (
    <PlannerContext.Provider value={value}>
      {children}
      <PlanningPage isOpen={isOpen} onClose={closePlanner} />
    </PlannerContext.Provider>
  );
}

export default PlannerProvider;
