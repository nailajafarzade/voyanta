import {
  listWishlist,
  addToWishlist as addToWishlistApi,
  removeFromWishlist as removeFromWishlistApi,
} from "../api/wishlist";
import { toPlace } from "../utils/destinations";
import {
  setFavorites,
  setWishlistLoading,
  toggleWishlist,
} from "./slice/wishlistSlice";

/** GET /api/wishlist → redux state (daxil olandan sonra çağırılır). */
export const loadWishlist = () => async (dispatch) => {
  dispatch(setWishlistLoading(true));
  try {
    const destinations = await listWishlist();
    dispatch(setFavorites(destinations.map(toPlace)));
  } catch (error) {
    console.error("Wishlist yüklənmədi:", error);
    dispatch(setFavorites([]));
  } finally {
    dispatch(setWishlistLoading(false));
  }
};

/**
 * Ürək düyməsi: əvvəl ekranda dəyişir (optimistic), sonra backend-ə yazılır
 * (POST/DELETE /api/wishlist/{id}). Backend xəta versə dəyişiklik geri qaytarılır.
 */
export const toggleFavorite = (place) => async (dispatch, getState) => {
  const exists = getState().wishlist.favorites.some((item) => item.id === place.id);

  dispatch(toggleWishlist(place));

  try {
    if (exists) {
      await removeFromWishlistApi(place.id);
    } else {
      await addToWishlistApi(place.id);
    }
  } catch (error) {
    dispatch(toggleWishlist(place));
    console.error("Wishlist dəyişdirilə bilmədi:", error);
  }
};
