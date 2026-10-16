import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  selectedPlace: null,
};

const informationSlice = createSlice({
  name: "information",
  initialState,
  reducers: {
    setSelectedPlace: (state, action) => {
      state.selectedPlace = action.payload;
    },

    clearSelectedPlace: (state) => {
      state.selectedPlace = null;
    },
  },
});

export const {
  setSelectedPlace,
  clearSelectedPlace,
} = informationSlice.actions;

export default informationSlice.reducer;