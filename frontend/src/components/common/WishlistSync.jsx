import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { useAuth } from "../../context/authContext";
import { clearFavorites } from "../../redux/slice/wishlistSlice";
import { loadWishlist } from "../../redux/wishlistThunks";

/**
 * Heç nə render etmir. İstifadəçi daxil olanda wishlist-i backend-dən yükləyir,
 * çıxış edəndə redux-dakı siyahını təmizləyir.
 */
function WishlistSync() {
  const dispatch = useDispatch();
  const { isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    if (isLoading) return;

    if (isAuthenticated) {
      dispatch(loadWishlist());
    } else {
      dispatch(clearFavorites());
    }
  }, [isAuthenticated, isLoading, dispatch]);

  return null;
}

export default WishlistSync;
