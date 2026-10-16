import PropTypes from "prop-types";
import { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { FaHeart, FaRegHeart } from "react-icons/fa";
import { useAuth } from "../../context/authContext";
import { useAuthModal } from "../../context/authModalContext";
import { toggleFavorite } from "../../redux/wishlistThunks";

function FavoriteButton({ place, size = "md", className = "", ariaLabel, variant = "default" }) {
    const dispatch = useDispatch();
    const { isAuthenticated, isLoading: authLoading } = useAuth();
    const { openLogin } = useAuthModal();
    const [popping, setPopping] = useState(false);
    const timer = useRef(null);

    const favorite = useSelector((state) =>
        state.wishlist.favorites.some((p) => p.id === place?.id)
    );
    const isUnavailable = !place?.id;

    useEffect(() => () => clearTimeout(timer.current), []);

    const handleClick = (e) => {
        e.stopPropagation();
        e.preventDefault();

        if (authLoading || !place?.id) return;

        if (!isAuthenticated) {
            openLogin();
            return;
        }

        setPopping(true);
        clearTimeout(timer.current);
        timer.current = setTimeout(() => setPopping(false), 300);

        dispatch(toggleFavorite(place));
    };

    const isSm = size === "sm";
    const iconSize = isSm ? "h-4 w-4" : "h-5 w-5";
    const buttonSize = isSm ? "h-9 w-9" : "h-11 w-11";

    const isGlass = variant === "glass";
    const baseClasses = `${buttonSize} inline-flex items-center justify-center rounded-full transition-all duration-300 hover:scale-110 active:scale-90 ${popping ? "scale-125" : ""} ${className}`;

    const inactiveClasses = isGlass
        ? "bg-white/20 text-white backdrop-blur-md hover:bg-white/35"
        : "bg-white/90 text-red-500 shadow-lg ring-1 ring-white/60 backdrop-blur-md";

    const activeClasses = isGlass
        ? "bg-white text-red-500 shadow-lg ring-1 ring-white/60 backdrop-blur-md"
        : "bg-white/90 text-red-500 shadow-lg ring-1 ring-white/60 backdrop-blur-md";

    return (
        <button
            type="button"
            onClick={handleClick}
            aria-label={ariaLabel || (favorite ? "Favoritlərdən sil" : "Favoritlərə əlavə et")}
            aria-pressed={favorite}
            aria-disabled={isUnavailable || undefined}
            title={isUnavailable ? "Bu yer üçün mövcud deyil" : undefined}
            className={`${baseClasses} ${isUnavailable ? "opacity-60 cursor-not-allowed" : ""} ${favorite ? activeClasses : inactiveClasses}`}
        >
            {favorite ? (
                <FaHeart className={iconSize} />
            ) : (
                <FaRegHeart className={iconSize} />
            )}
        </button>
    );
}

FavoriteButton.propTypes = {
    place: PropTypes.shape({ id: PropTypes.string }),
    size: PropTypes.oneOf(["sm", "md"]),
    className: PropTypes.string,
    ariaLabel: PropTypes.string,
    variant: PropTypes.oneOf(["default", "glass"]),
};

export default FavoriteButton;
