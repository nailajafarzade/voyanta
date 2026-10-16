package com.voyanta.wishlist.service;

import com.voyanta.common.exception.ResourceNotFoundException;
import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.image.DestinationImageQuery;
import com.voyanta.image.ImageService;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import com.voyanta.wishlist.dao.entity.WishlistItem;
import com.voyanta.wishlist.dao.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final DestinationRepository destinationRepository;
    private final ImageService imageService;
    /** Destinations created from an AI plan carry this tag; their stored cover image wins. */
    private static final String PLAN_TAG = "Səyahət planı";

    @Transactional
    public void add(UUID userId, UUID destinationId) {
        if (!destinationRepository.existsById(destinationId)) {
            throw new ResourceNotFoundException("Destinasiya tapılmadı");
        }

        // Artıq wishlist-dədirsə səssizcə çıxırıq — frontend "əvvəlcə vəziyyəti yoxla,
        // sonra əlavə et" məcburiyyətində qalmır, ikonu neçə dəfə bassa da nəticə eynidir.
        if (wishlistRepository.existsByUserIdAndDestinationId(userId, destinationId)) {
            return;
        }

        wishlistRepository.save(WishlistItem.builder()
                .userId(userId)
                .destinationId(destinationId)
                .build());
    }

    @Transactional
    public void remove(UUID userId, UUID destinationId) {
        wishlistRepository.deleteByUserIdAndDestinationId(userId, destinationId);
    }

    @Transactional(readOnly = true)
    public List<DestinationResponse> list(UUID userId) {
        List<WishlistItem> items = wishlistRepository.findByUserId(userId);
        List<UUID> destinationIds = items.stream().map(WishlistItem::getDestinationId).toList();

        Map<UUID, Destination> destinationsById = destinationRepository.findAllById(destinationIds).stream()
                .collect(Collectors.toMap(Destination::getId, d -> d));

        return items.stream()
                .map(item -> destinationsById.get(item.getDestinationId()))
                .filter(Objects::nonNull) // destinasiya sonradan silinmiş ola bilər — səssizcə keç
                .map(this::toResponse)
                .collect(Collectors.toList());
    }



    /** Same image resolution as the other destination card lists; DB value as fallback. */
    private DestinationResponse toResponse(Destination d) {
        List<ImageCandidateResponse> images = imageService.resolveImageCandidates(
                DestinationImageQuery.of(d.getName(), d.getCountry(), d.getTag(), d.getInterestTags()));

        // AI planından yaranan destinasiya: wishlist-də də planda göstərilən eyni şəkil çıxsın
        if (PLAN_TAG.equals(d.getTag()) && d.getImageUrl() != null) {
            return new DestinationResponse(
                    d.getId(), d.getName(), d.getCountry(), d.getImageUrl(), d.getTag(), images
            );
        }

        String best = images.isEmpty() ? null : images.get(0).url();
        return new DestinationResponse(
                d.getId(), d.getName(), d.getCountry(),
                best != null ? best : d.getImageUrl(), d.getTag(), images
        );
    }
}