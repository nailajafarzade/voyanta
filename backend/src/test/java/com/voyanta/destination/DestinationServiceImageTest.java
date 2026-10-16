package com.voyanta.destination;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.destination.service.DestinationService;
import com.voyanta.image.ImageService;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The homepage-only card lists ({@code /batch} and {@code ?season=}) are rendered with
 * hardcoded images in the frontend, so they must NOT hit the Unsplash image provider.
 * The featured list is shared with non-homepage pages and still resolves images.
 */
@ExtendWith(MockitoExtension.class)
class DestinationServiceImageTest {

    @Mock
    private DestinationRepository destinationRepository;

    @Mock
    private ImageService imageService;

    private DestinationService service;
    private Destination beach;

    @BeforeEach
    void setUp() {
        service = new DestinationService(destinationRepository, imageService);
        beach = Destination.builder()
                .id(UUID.randomUUID())
                .name("Beach")
                .country("Sea")
                .imageUrl("https://db/beach.jpg")
                .tag("Dəniz")
                .season("Yay")
                .build();
    }

    @Test
    void batchAndSeasonListsSkipTheImageProvider() {
        when(destinationRepository.findAllById(any())).thenReturn(List.of(beach));
        when(destinationRepository.findAll()).thenReturn(List.of(beach));

        List<DestinationResponse> batch = service.getByIds(List.of(beach.getId()));
        List<DestinationResponse> season = service.getBySeason("Yay", 4);

        verifyNoInteractions(imageService);

        assertThat(batch).hasSize(1);
        assertThat(batch.get(0).imageUrl()).isEqualTo("https://db/beach.jpg");
        assertThat(batch.get(0).images()).isEmpty();

        assertThat(season).hasSize(1);
        assertThat(season.get(0).imageUrl()).isEqualTo("https://db/beach.jpg");
        assertThat(season.get(0).images()).isEmpty();
    }

    @Test
    void featuredListStillResolvesImagesFromTheProvider() {
        when(destinationRepository.findByFeaturedTrue()).thenReturn(List.of(beach));
        when(imageService.resolveImageCandidates(any())).thenReturn(List.of(
                new ImageCandidateResponse("1", "https://img/beach", "https://img/beach-full",
                        "Author", null, null)));

        List<DestinationResponse> featured = service.getFeatured(null);

        assertThat(featured).hasSize(1);
        assertThat(featured.get(0).imageUrl()).isEqualTo("https://img/beach");
        assertThat(featured.get(0).images()).hasSize(1);
    }

    @Test
    void batchWithoutIdsReturnsEmptyWithoutTouchingTheProvider() {
        assertThat(service.getByIds(List.of())).isEmpty();
        verifyNoInteractions(imageService);
    }
}
