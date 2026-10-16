package com.voyanta.destination.dto.response;

import com.voyanta.image.dto.response.ImageCandidateResponse;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a destination card.
 *
 * <p><b>imageUrl</b> is the best (first) candidate URL — kept so the current
 * frontend keeps working unchanged. <b>images</b> carries ALL relevant
 * candidates, so several cards showing the same destination can each get a
 * different photo.
 *
 * @param id       destination ID
 * @param name     name
 * @param country  country
 * @param imageUrl best image URL (falls back to the stored DB value)
 * @param tag      card tag
 * @param images   relevant image candidates (may be empty)
 */
public record DestinationResponse(
        UUID id,
        String name,
        String country,
        String imageUrl,
        String tag,
        List<ImageCandidateResponse> images
) {

    /** Backward-compatible constructor for callers that only have a single URL. */
    public DestinationResponse(UUID id, String name, String country, String imageUrl, String tag) {
        this(id, name, country, imageUrl, tag, List.of());
    }
}