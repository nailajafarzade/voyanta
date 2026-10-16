import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  favorites: [],
  isLoading: false,
};

const wishlistSlice = createSlice({
  name: "wishlist",
  initialState,

  reducers: {
    addToWishlist: (state, action) => {
      const exists = state.favorites.some(
        (item) => item.id === action.payload.id
      );

      if (!exists) {
        state.favorites.push(action.payload);
      }
    },

    removeFromWishlist: (state, action) => {
      state.favorites = state.favorites.filter(
        (item) => item.id !== action.payload
      );
    },

    toggleWishlist: (state, action) => {
      const exists = state.favorites.some(
        (item) => item.id === action.payload.id
      );

      if (exists) {
        state.favorites = state.favorites.filter(
          (item) => item.id !== action.payload.id
        );
      } else {
        state.favorites.push(action.payload);
      }
    },

    // Backend-dən gələn siyahı ilə tam əvəz edir
    setFavorites: (state, action) => {
      state.favorites = action.payload;
    },

    clearFavorites: (state) => {
      state.favorites = [];
    },

    setWishlistLoading: (state, action) => {
      state.isLoading = action.payload;
    },
  },
});

export const {
  addToWishlist,
  removeFromWishlist,
  toggleWishlist,
  setFavorites,
  clearFavorites,
  setWishlistLoading,
} = wishlistSlice.actions;

export default wishlistSlice.reducer;
