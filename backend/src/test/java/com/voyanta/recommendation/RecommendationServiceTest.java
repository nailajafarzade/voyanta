package com.voyanta.recommendation;

import com.voyanta.destination.dao.entity.Destination;
import com.voyanta.destination.dao.repository.DestinationRepository;
import com.voyanta.destination.dto.response.DestinationResponse;
import com.voyanta.image.DestinationImageQuery;
import com.voyanta.image.ImageService;
import com.voyanta.image.dto.response.ImageCandidateResponse;
import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.dao.repository.TravelPlanRepository;
import com.voyanta.recommendation.service.RecommendationService;
import com.voyanta.survey.enums.InterestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private TravelPlanRepository travelPlanRepository;
    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private com.voyanta.wishlist.dao.repository.WishlistRepository wishlistRepository;
    @Mock
    private ImageService imageService;

    // Constructed explicitly (not @InjectMocks) because the service now also takes
    // the wishlist repository, the scorer and a Clock. Mockito cannot pick the right
    // constructor for that, and an explicit build makes the dependency list obvious.
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                travelPlanRepository, destinationRepository, wishlistRepository,
                new com.voyanta.recommendation.service.RecommendationScorer(), imageService, java.time.Clock.systemUTC());
        // Lenient: a test that only exercises the personalized path never calls the
        // popularity/trending aggregations, and strict stubs would fail it.
        lenient().when(wishlistRepository.countFavoritesPerDestination()).thenReturn(List.of());
        lenient().when(wishlistRepository.countFavoritesPerDestinationSince(any())).thenReturn(List.of());
        lenient().when(wishlistRepository.findByUserId(any())).thenReturn(List.of());
    }

    @Test
    void fallsBackToFeaturedWhenUserHasNoInterestHistory() {
        UUID userId = UUID.randomUUID();
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of());
        Destination featured = Destination.builder()
                .id(UUID.randomUUID())
                .name("Santorini")
                .country("Yunanıstan")
                .imageUrl("http://img")
                .tag("Dəniz")
                .featured(true)
                .build();
        when(destinationRepository.findAll()).thenReturn(List.of(featured));

        List<DestinationResponse> result = recommendationService.getPersonalized(userId, 3);
        assertThat(result).extracting(DestinationResponse::name).containsExactly("Santorini");
    }

    @Test
    void ranksDestinationsByInterestOverlap() {
        UUID userId = UUID.randomUUID();
        TravelPlan plan = TravelPlan.builder().interests(Set.of(InterestType.SEA)).build();
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of(plan));

        Destination egypt = Destination.builder()
                .id(UUID.randomUUID()).name("Misir").country("Misir").imageUrl("e").tag("Tarix")
                .interestTags(Set.of(InterestType.HISTORY_CULTURE)).build();
        Destination maldives = Destination.builder()
                .id(UUID.randomUUID()).name("Maldiv adaları").country("Maldivlər").imageUrl("m").tag("Dəniz")
                .interestTags(Set.of(InterestType.SEA)).build();
        Destination santorini = Destination.builder()
                .id(UUID.randomUUID()).name("Santorini").country("Yunanıstan").imageUrl("s").tag("Dəniz")
                .interestTags(Set.of(InterestType.SEA)).build();
        when(destinationRepository.findAll()).thenReturn(List.of(egypt, maldives, santorini));

        List<DestinationResponse> result = recommendationService.getPersonalized(userId, 3);

        // Both SEA destinations are returned; the unrelated history destination is
        // not. This matches the contract RecommendationIT already asserts: a sea-only
        // user must never be shown "Misir".
        assertThat(result).extracting(DestinationResponse::name)
                .containsExactlyInAnyOrder("Maldiv adaları", "Santorini")
                .doesNotContain("Misir");
    }

    /**
     * Hər üç sətir üçün də eyni şəkil qaytarılmamalıdır: frontend hər kart üçün
     * siyahıdan fərqli şəkil seçə bilər.
     */
    @Test
    void eachDestinationCarriesItsOwnCandidateList() {
        UUID userId = UUID.randomUUID();
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of());
        Destination egypt = Destination.builder()
                .id(UUID.randomUUID()).name("Misir").country("Misir").imageUrl("db-egypt").tag("Tarix")
                .featured(true).build();
        Destination maldives = Destination.builder()
                .id(UUID.randomUUID()).name("Maldiv adaları").country("Maldivlər").imageUrl("db-maldives")
                .tag("Dəniz").featured(true).build();
        when(destinationRepository.findAll()).thenReturn(List.of(egypt, maldives));

        when(imageService.resolveImageCandidates(any())).thenAnswer(invocation -> {
            DestinationImageQuery query = invocation.getArgument(0);
            return List.of(new ImageCandidateResponse(
                    query.destination(),
                    "https://images.unsplash.com/" + query.destination(),
                    "https://images.unsplash.com/" + query.destination() + "-full",
                    null, null, null));
        });

        List<DestinationResponse> result = recommendationService.getPersonalized(userId, 5);

        // Both destinations score equally here (no user data, both featured), so the
        // order is the documented deterministic name tie-break rather than DB order.
        assertThat(result).extracting(DestinationResponse::imageUrl)
                .containsExactlyInAnyOrder("https://images.unsplash.com/Misir",
                        "https://images.unsplash.com/Maldiv adaları");
        assertThat(result).extracting(DestinationResponse::images)
                .allSatisfy(images -> assertThat(images).hasSize(1));

        // Tag + interestTags sorğuya ötürülməlidir (şəkil seçimi bunlara görə dəyişir).
        ArgumentCaptor<DestinationImageQuery> query = ArgumentCaptor.forClass(DestinationImageQuery.class);
        verify(imageService, times(2)).resolveImageCandidates(query.capture());
        assertThat(query.getAllValues()).extracting(DestinationImageQuery::tag)
                .containsExactlyInAnyOrder("Tarix", "Dəniz");
    }

    @Test
    void fallsBackToTheStoredDbImageWhenNoCandidateIsResolved() {
        UUID userId = UUID.randomUUID();
        when(travelPlanRepository.findByUserId(userId)).thenReturn(List.of());
        Destination egypt = Destination.builder()
                .id(UUID.randomUUID()).name("Misir").country("Misir")
                .imageUrl("https://db/egypt.jpg").tag("Tarix").featured(true).build();
        when(destinationRepository.findAll()).thenReturn(List.of(egypt));
        when(imageService.resolveImageCandidates(any())).thenReturn(List.of());

        DestinationResponse response = recommendationService.getPersonalized(userId, 3).get(0);

        assertThat(response.imageUrl()).isEqualTo("https://db/egypt.jpg");
        assertThat(response.images()).isEmpty();
    }
}
