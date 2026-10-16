import { configureStore } from "@reduxjs/toolkit";
import {
  persistStore,
  persistReducer,
  FLUSH,
  REHYDRATE,
  PAUSE,
  PERSIST,
  PURGE,
  REGISTER,
} from "redux-persist";
import storage from "redux-persist/lib/storage";

import informationReducer from "./slice/informationSlice";
import wishlistReducer from "./slice/wishlistSlice";

const informationPersistConfig = {
  key: "information",
  storage,
};

const persistedInformationReducer = persistReducer(
  informationPersistConfig,
  informationReducer
);

// Wishlist artıq backend-dədir (GET/POST/DELETE /api/wishlist) və hər istifadəçiyə aiddir,
// ona görə brauzerdə saxlanmır — əks halda başqa hesabın favoritləri görünə bilərdi.
export const store = configureStore({
  reducer: {
    information: persistedInformationReducer,
    wishlist: wishlistReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
});

export const persistor = persistStore(store);
