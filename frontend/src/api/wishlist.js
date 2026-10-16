import api, { unwrap } from "./axios";
/** GET /api/wishlist — AUTH tələb olunur. */
export async function listWishlist() {
    return unwrap(await api.get("/wishlist")) ?? [];
}
/** POST /api/wishlist/{destinationId} — AUTH tələb olunur. */
export async function addToWishlist(destinationId) {
    await api.post(`/wishlist/${destinationId}`);
}
/** DELETE /api/wishlist/{destinationId} — AUTH tələb olunur. */
export async function removeFromWishlist(destinationId) {
    await api.delete(`/wishlist/${destinationId}`);
}
