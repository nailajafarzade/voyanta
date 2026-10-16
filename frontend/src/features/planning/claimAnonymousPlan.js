import { claimPlan } from "../../api/plans";
import { toApiError } from "../../api/errors";
import { clearPendingPlanId, getPendingPlanId, isPlanClaimed, markPlanClaimed } from "./storage";
/**
 * Daxil olduqdan/qeydiyyatdan keçdikdən sonra çağırılır: anonim yaradılmış plan
 * varsa hesaba bağlayır. Qaytarılan dəyər — açılmış planın id-si (varsa),
 * çağıran tərəf ona yönləndirə bilər.
 *
 * PLAN_ALREADY_CLAIMED (403) və RESOURCE_NOT_FOUND (404) halları xəta kimi yuxarı
 * ötürülmür — istifadəçi üçün giriş uğurludur, sadəcə plan artıq bağlıdır.
 */
export async function claimAnonymousPlan() {
    const planId = getPendingPlanId();
    if (!planId)
        return null;
    if (isPlanClaimed(planId)) {
        clearPendingPlanId();
        return planId;
    }
    try {
        await claimPlan(planId);
        markPlanClaimed(planId);
        clearPendingPlanId();
        return planId;
    }
    catch (error) {
        const apiError = toApiError(error);
        if (apiError.errorCode === "PLAN_ALREADY_CLAIMED") {
            // Təkrar cəhd etməyin mənası yoxdur
            markPlanClaimed(planId);
            clearPendingPlanId();
            return planId;
        }
        if (apiError.errorCode === "RESOURCE_NOT_FOUND") {
            clearPendingPlanId();
            return null;
        }
        console.error("Plan claim edilə bilmədi:", apiError);
        return null;
    }
}
